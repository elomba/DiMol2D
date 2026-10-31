#!/usr/bin/env python3
"""
DiMol2D v1.1 - Interactive 2D Molecular Dynamics Visualization Suite (PyQt5 + Numba).

Originally designed in Java Swing + C JNI + Fortran 90 by:
    E. Lomba, N. G. Almarza, and C. Martín (IQFR-CSIC, Madrid, Spain)

Architecture Overview
---------------------
- `LabeledSlider`: Custom QSlider wrapper rendering aligned major/minor ticks and numeric scales.
- `GraphHelper`: Scientific 2D plotting helper for axes, ticks, curves, polygons, and markers.
- `PintaRTWidget` & `ManometerWidget`: Real-time digital (T*, rho*) readout and gradient pressure gauge.
- `GrWindow`, `PdxWindow`, `PdyWindow`, `PhaseDiagramWindow`: Live structural and thermodynamic plots
  for radial distribution functions g(r), lateral/vertical wall density profiles, and phase diagrams.
- `InteraccionWindow`, `InteraccionWallWindow`, `ComposicionWindow`, `ElongacionWindow`,
  `SetNpartWindow`, `SetVelocWindow`: Interactive parameter control dialogs.
- `HelpWindow` & `AboutWindow`: Embedded HTML help browser (Spanish/English) and credits dialog.
- `SimulationWidget`: Core 2D canvas driving the Numba `MDEngine` via a QTimer loop, rendering
  monomers, homonuclear/heteronuclear diatomic dumbbells, dynamic bonds, and confining walls.
- `DiMol2DMainWindow`: Top-level application window managing menus, sliders, and child windows.
"""

import argparse
import locale
import math
import os
import sys
import numpy as np

from PyQt5.QtCore import Qt, QTimer, QPoint, QRect, QUrl
from PyQt5.QtGui import (
    QColor, QFont, QFontMetrics, QLinearGradient, QPainter,
    QPainterPath, QPen, QBrush, QPixmap, QPolygon
)
from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QLabel, QSlider,
    QMenu, QAction, QActionGroup, QMessageBox, QInputDialog,
    QVBoxLayout, QHBoxLayout, QGridLayout, QTextBrowser, QDialog,
    QStyle, QStyleOptionSlider
)
from PyQt5.QtPrintSupport import QPrinter, QPrintDialog

from md_engine import MDEngine
import phase_data as pd_data

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# High-contrast color palette
COLOR_A = QColor(235, 95, 0)       # Vibrant orange for component A
COLOR_B = QColor(30, 130, 45)      # Rich green for component B
COLOR_C = QColor(45, 175, 205)     # Distinct cyan-teal for component C (visible on white plots & canvas)
COLOR_W = QColor(175, 195, 210)    # Wall fill color
COLOR_FND = QColor(228, 236, 242)  # Clean light slate-gray panel background for high text legibility
COLOR_BP = QColor(255, 255, 255)   # White plot background
COLOR_TEXT = QColor(20, 32, 48)    # High-contrast dark navy/charcoal text
COLOR_BAR_BG = QColor(12, 98, 145) # Deep blue header/menubar background
COLOR_BAR_FG = QColor(255, 245, 150)

GLOBAL_STYLESHEET = """
QWidget {
    color: rgb(20, 32, 48);
    font-family: Helvetica, Arial, sans-serif;
}
QMainWindow, QDialog {
    background-color: rgb(228, 236, 242);
}
QLabel {
    color: rgb(20, 32, 48);
    background: transparent;
}
QMenuBar {
    background-color: rgb(12, 98, 145);
    color: rgb(255, 245, 150);
    font-weight: bold;
    font-size: 13px;
    padding: 2px;
}
QMenuBar::item {
    background: transparent;
    padding: 4px 10px;
}
QMenuBar::item:selected {
    background-color: rgb(8, 72, 110);
    color: white;
}
QMenu {
    background-color: rgb(250, 252, 245);
    color: rgb(20, 32, 48);
    border: 1px solid rgb(120, 140, 160);
    font-size: 13px;
}
QMenu::item {
    padding: 5px 24px 5px 20px;
    color: rgb(20, 32, 48);
}
QMenu::item:selected {
    background-color: rgb(12, 98, 145);
    color: rgb(255, 245, 150);
}
QMenu::item:disabled {
    color: rgb(150, 160, 170);
}
QPushButton, QLineEdit, QDoubleSpinBox {
    background-color: white;
    color: rgb(20, 32, 48);
}
"""


def resource_path(rel_path: str) -> str:
    return os.path.join(BASE_DIR, rel_path.lstrip("/"))


def load_messages(lang: str = None) -> dict:
    """Load localized strings from MessagesBundle[_es].properties."""
    if lang is None:
        loc, _ = locale.getdefaultlocale()
        lang = "es" if (loc and loc.lower().startswith("es")) else "en"
    fname = "MessagesBundle_es.properties" if lang.startswith("es") else "MessagesBundle.properties"
    fpath = resource_path(fname)
    if not os.path.exists(fpath):
        fpath = resource_path("MessagesBundle.properties")

    messages = {}
    with open(fpath, "rb") as f:
        raw = f.read()
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        text = raw.decode("iso-8859-1")

    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        messages[k.strip()] = v.strip()
    return messages


# =============================================================================
# Custom Slider with Drawn Tick Marks and Numeric Scale Labels
# =============================================================================
class LabeledSlider(QWidget):
    """
    A QSlider paired with custom-painted major/minor tick marks and numeric scale labels,
    replicating Java Swing's JSlider.setPaintTicks(true) + setPaintLabels(true).
    """

    def __init__(
        self,
        orientation=Qt.Vertical,
        minimum: int = 0,
        maximum: int = 100,
        value: int = 0,
        major_step: int = 20,
        minor_step: int = 10,
        scale_factor: float = 0.01,
        decimals: int = 1,
        parent=None
    ):
        super().__init__(parent)
        self.orientation = orientation
        self.major_step = major_step
        self.minor_step = minor_step
        self.scale_factor = scale_factor
        self.decimals = decimals

        self.slider = QSlider(orientation, self)
        self.slider.setRange(minimum, maximum)
        self.slider.setValue(value)
        self.slider.setTickPosition(QSlider.NoTicks)

        if orientation == Qt.Vertical:
            layout = QHBoxLayout(self)
            layout.setContentsMargins(2, 6, 36, 6)
            layout.setSpacing(0)
            layout.addWidget(self.slider)
            self.setMinimumWidth(68)
        else:
            layout = QVBoxLayout(self)
            layout.setContentsMargins(10, 2, 10, 24)
            layout.setSpacing(0)
            layout.addWidget(self.slider)
            self.setMinimumHeight(52)

    # Proxy common QSlider methods
    @property
    def valueChanged(self):
        return self.slider.valueChanged

    @property
    def sliderReleased(self):
        return self.slider.sliderReleased

    def value(self) -> int:
        return self.slider.value()

    def setValue(self, val: int):
        self.slider.setValue(val)
        self.update()

    def setEnabled(self, enabled: bool):
        super().setEnabled(enabled)
        self.slider.setEnabled(enabled)
        self.update()

    def blockSignals(self, b: bool) -> bool:
        return self.slider.blockSignals(b)

    def setSingleStep(self, step: int):
        self.slider.setSingleStep(step)

    def setPageStep(self, step: int):
        self.slider.setPageStep(step)

    def _format_val(self, v: int) -> str:
        scaled = v * self.scale_factor
        if self.decimals == 0:
            return f"{int(round(scaled))}"
        return f"{scaled:.{self.decimals}f}"

    def paintEvent(self, event):
        super().paintEvent(event)
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)

        opt = QStyleOptionSlider()
        self.slider.initStyleOption(opt)
        style = self.slider.style()
        groove = style.subControlRect(QStyle.CC_Slider, opt, QStyle.SC_SliderGroove, self.slider)
        handle = style.subControlRect(QStyle.CC_Slider, opt, QStyle.SC_SliderHandle, self.slider)

        vmin = self.slider.minimum()
        vmax = self.slider.maximum()
        if vmax <= vmin:
            return

        pen_color = COLOR_TEXT if self.isEnabled() else QColor(130, 140, 150)
        p.setPen(QPen(pen_color, 1.2))
        font = QFont("Helvetica", 9, QFont.Bold)
        p.setFont(font)
        fm = QFontMetrics(font)

        if self.orientation == Qt.Vertical:
            slider_pos = self.slider.pos()
            half_h = handle.height() // 2
            y_top = slider_pos.y() + groove.top() + half_h
            y_bot = slider_pos.y() + groove.bottom() - half_h
            span = y_bot - y_top
            x_base = slider_pos.x() + self.slider.width() + 2

            step = self.minor_step if self.minor_step > 0 else self.major_step
            val = vmin
            while val <= vmax:
                frac = (val - vmin) / float(vmax - vmin)
                y = int(round(y_bot - frac * span))
                is_major = (self.major_step > 0) and ((val - vmin) % self.major_step == 0)
                tick_len = 7 if is_major else 4
                p.drawLine(x_base, y, x_base + tick_len, y)
                if is_major:
                    lbl = self._format_val(val)
                    p.drawText(x_base + tick_len + 3, y + fm.ascent() // 2 - 1, lbl)
                val += step
        else:
            slider_pos = self.slider.pos()
            half_w = handle.width() // 2
            x_left = slider_pos.x() + groove.left() + half_w
            x_right = slider_pos.x() + groove.right() - half_w
            span = x_right - x_left
            y_base = slider_pos.y() + self.slider.height() + 1

            step = self.minor_step if self.minor_step > 0 else self.major_step
            val = vmin
            while val <= vmax:
                frac = (val - vmin) / float(vmax - vmin)
                x = int(round(x_left + frac * span))
                is_major = (self.major_step > 0) and ((val - vmin) % self.major_step == 0)
                tick_len = 6 if is_major else 3
                p.drawLine(x, y_base, x, y_base + tick_len)
                if is_major:
                    lbl = self._format_val(val)
                    tw = fm.horizontalAdvance(lbl)
                    p.drawText(x - tw // 2, y_base + tick_len + fm.ascent() + 2, lbl)
                val += step


# =============================================================================
# Reusable 2D Scientific Graph Helper (translation of Graph.java)
# =============================================================================
class GraphHelper:
    """Draws axes, ticks, lines, dashed lines, polygons, and points onto a QPainter."""

    def draw_graph(
        self, p: QPainter, ihs: int, ivs: int, vs: int, hs: int,
        xmin: float, xmax: float, ymin: float, ymax: float,
        label_y: str, label_x: str, baseline: float
    ):
        a = (xmax - xmin) / float(hs)
        b = xmin - a * float(ihs)
        ap = -(ymax - ymin) / float(vs)
        bp = ymax - ap * float(ivs)

        p.setPen(COLOR_TEXT)
        p.setFont(QFont("Helvetica", 11, QFont.Bold))
        p.drawText(ihs - 4, vs + (3 * ivs) // 2, f"{xmin:.1f}")
        p.drawText(ihs + hs - 12, vs + (3 * ivs) // 2, f"{xmax:.1f}")
        p.drawText(max(4, ihs - 38), ivs + 6, f"{ymax:.1f}")
        p.drawText(max(4, ihs - 38), vs + ivs + 4, f"{ymin:.1f}")

        p.setFont(QFont("Times", 12, QFont.Bold))
        p.setPen(QColor(165, 15, 45))
        p.drawText(ihs + hs // 2 - 8, 12 + vs + (3 * ivs) // 2, label_x)
        p.drawText(max(4, ihs - 52), ivs + vs // 2, label_y)

        p.setPen(QPen(COLOR_TEXT, 1.3))
        p.setBrush(Qt.NoBrush)
        p.drawRect(ihs, ivs, hs, vs)
        self.set_ticks(p, 4, 4, ihs, ivs + vs, hs, vs, 0)
        self.set_ticks(p, 4, 4, ihs, ivs + 10, hs, vs + 10, 1)
        self.set_ticks(p, 4, 4, ihs + hs - 10, ivs + vs, hs, vs, 2)

        y_base = int((baseline - bp) / ap + 0.51)
        if ivs <= y_base <= ivs + vs:
            p.drawLine(int((xmin - b) / a + 0.51), y_base, int((xmax - b) / a + 0.51), y_base)

    def draw_graph_line(
        self, p: QPainter, x, y, nmn: int, n: int,
        ihs: int, ivs: int, vs: int, hs: int,
        xmin: float, xmax: float, ymin: float, ymax: float,
        ixl: int, iyl: int, ixl1: int, iyl1: int, label: str, color: QColor
    ):
        if n < nmn:
            return
        a = (xmax - xmin) / float(hs)
        b = xmin - a * float(ihs)
        ap = -(ymax - ymin) / float(vs)
        bp = ymax - ap * float(ivs)

        p.setPen(QPen(color, 2.0))
        if label:
            p.drawLine(ixl, iyl, ixl1, iyl1)
            p.setPen(COLOR_TEXT)
            p.setFont(QFont("Helvetica", 10, QFont.Bold))
            p.drawText(ixl1 + 6, iyl1 + 5, label)
            p.setPen(QPen(color, 2.0))

        ix0 = int((x[nmn] - b) / a + 0.51)
        iy0 = int((y[nmn] - bp) / ap + 0.51)
        for i in range(nmn, n):
            ix1 = int((x[i + 1] - b) / a + 0.51)
            iy1 = int((y[i + 1] - bp) / ap + 0.51)
            p.drawLine(ix0, iy0, ix1, iy1)
            ix0, iy0 = ix1, iy1

    def draw_dash_line(
        self, p: QPainter, x, y, nmn: int, n: int,
        ihs: int, ivs: int, vs: int, hs: int,
        xmin: float, xmax: float, ymin: float, ymax: float, color: QColor
    ):
        a = (xmax - xmin) / float(hs)
        b = xmin - a * float(ihs)
        ap = -(ymax - ymin) / float(vs)
        bp = ymax - ap * float(ivs)

        pen = QPen(color, 1.4, Qt.DashLine)
        p.setPen(pen)
        ix0 = int((x[nmn] - b) / a + 0.51)
        iy0 = int((y[nmn] - bp) / ap + 0.51)
        for i in range(nmn, n):
            ix1 = int((x[i + 1] - b) / a + 0.51)
            iy1 = int((y[i + 1] - bp) / ap + 0.51)
            p.drawLine(ix0, iy0, ix1, iy1)
            ix0, iy0 = ix1, iy1

    def draw_graph_poly(
        self, p: QPainter, x, y, nmn: int, n: int,
        ihs: int, ivs: int, vs: int, hs: int,
        xmin: float, xmax: float, ymin: float, ymax: float, fill_color: QColor
    ):
        a = (xmax - xmin) / float(hs)
        b = xmin - a * float(ihs)
        ap = -(ymax - ymin) / float(vs)
        bp = ymax - ap * float(ivs)

        poly = QPolygon()
        for i in range(nmn, n + 1):
            ix = int((x[i] - b) / a + 0.51)
            iy = int((y[i] - bp) / ap + 0.51)
            poly.append(QPoint(ix, iy))
        p.setPen(Qt.NoPen)
        p.setBrush(QBrush(fill_color))
        p.drawPolygon(poly)

    def draw_graph_point(
        self, p: QPainter, x: float, y: float,
        ihs: int, ivs: int, vs: int, hs: int,
        xmin: float, xmax: float, ymin: float, ymax: float,
        color: QColor, filled: bool = True
    ):
        a = (xmax - xmin) / float(hs)
        b = xmin - a * float(ihs)
        ap = -(ymax - ymin) / float(vs)
        bp = ymax - ap * float(ivs)
        itam = 8
        ix0 = int((x - b) / a + 0.51)
        iy0 = int((y - bp) / ap + 0.51)
        p.setPen(QPen(color, 1.8))
        p.setBrush(QBrush(color) if filled else Qt.NoBrush)
        p.drawEllipse(ix0 - itam // 2, iy0 - itam // 2, itam, itam)

    def set_ticks(
        self, p: QPainter, ntx: int, nty: int,
        ixor: int, iyor: int, hsize: int, vsize: int, kaso: int
    ):
        if kaso == 0:
            idx = hsize // (ntx + 1)
            for i in range(1, ntx + 1):
                p.drawLine(ixor + i * idx, iyor, ixor + i * idx, iyor - 10)
            idy = vsize // (nty + 1)
            for i in range(1, nty + 1):
                p.drawLine(ixor, iyor - i * idy, ixor + 10, iyor - i * idy)
        elif kaso == 1:
            idx = hsize // (ntx + 1)
            for i in range(1, ntx + 1):
                p.drawLine(ixor + i * idx, iyor, ixor + i * idx, iyor - 10)
        elif kaso == 2:
            idy = vsize // (nty + 1)
            for i in range(1, nty + 1):
                p.drawLine(ixor, iyor - i * idy, ixor + 10, iyor - i * idy)


# =============================================================================
# Right-Panel Widgets: PintaRT & Manometer
# =============================================================================
class PintaRTWidget(QWidget):
    """Displays current reduced Temperature and Density (PintaRT.java)."""

    def __init__(self, messages: dict, temp: float = 0.0, dens: float = 0.0, width: int = 148, height: int = 56):
        super().__init__()
        self.messages = messages
        self.temp = temp
        self.dens = dens
        self.setFixedSize(width, height)

    def set_rt(self, temp: float, dens: float):
        self.temp = temp
        self.dens = dens
        self.update()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), QColor(212, 224, 235))
        p.setPen(QPen(QColor(140, 165, 190), 1.2))
        p.drawRoundedRect(1, 1, self.width() - 2, self.height() - 2, 5, 5)
        p.setPen(QColor(15, 35, 95))
        p.setFont(QFont("Helvetica", 10, QFont.Bold))
        p.drawText(8, 22, f"{self.messages.get('temperatura', 'Temperature')} = {self.temp:.2f}")
        p.drawText(8, 43, f"{self.messages.get('densidad', 'Density')} = {self.dens:.2f}")


class ManometerWidget(QWidget):
    """Displays pressure gauge with combined linear/logarithmic scale (Manometer.java)."""

    def __init__(self, messages: dict, pres: float = 0.0, ancho: int = 16, alto: int = 230):
        super().__init__()
        self.messages = messages
        self.pres = pres
        self.X0 = 32
        self.Y0 = 34
        self.ANCHO = ancho
        self.ALTO = alto
        self.PMIN = -1.0
        self.PMAXL = 1.0
        self.PMAX = 40.0
        self.NTICK = 20
        self.NMTICK = 10
        self.NTICKL = 20
        self.setFixedSize(self.X0 + self.ANCHO + 92, self.Y0 + self.ALTO + 26)

    def set_pressure(self, pres: float):
        self.pres = pres
        self.update()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)

        x1 = self.X0 + self.ANCHO
        y1 = self.Y0 + self.ALTO
        altol = self.ALTO // 2
        xdim = self.width()
        ydim = self.height()

        grad = QLinearGradient(0, 0, xdim, ydim)
        grad.setColorAt(0.0, QColor(12, 38, 95))
        grad.setColorAt(1.0, QColor(18, 82, 148))
        p.fillRect(0, 0, xdim, ydim, QBrush(grad))

        p.setPen(QColor(255, 242, 155))
        p.setFont(QFont("Helvetica", 10, QFont.Bold))
        p.drawText(18, 21, f"{self.messages.get('presion', 'Pressure')} ({self.pres:.2f})")

        if self.pres <= self.PMAXL:
            altura = int(altol * (self.pres - self.PMIN) / (self.PMAXL - self.PMIN) + 0.51)
        else:
            altura = int(
                (self.ALTO - altol)
                * (math.log(max(self.pres, self.PMAXL)) - math.log(self.PMAXL))
                / (math.log(self.PMAX) - math.log(self.PMAXL))
                + 0.51
            ) + altol
        altura = max(0, min(self.ALTO, altura))

        p.fillRect(self.X0, y1 - altura, self.ANCHO, altura, QColor(195, 225, 255))
        p.setPen(QPen(QColor(255, 85, 85), 2.2))
        p.setBrush(Qt.NoBrush)
        p.drawRect(self.X0, self.Y0, self.ANCHO, self.ALTO)

        p.setPen(QPen(QColor(255, 240, 120), 1.5))
        p.setFont(QFont("Helvetica", 9, QFont.Bold))
        p.drawLine(x1 + 10, self.Y0, x1 + 10, y1)

        dy = (self.PMAXL - self.PMIN) / float(self.NTICK)
        da = float(altol) / float(self.NTICK)
        for i in range(self.NTICK + 1):
            ytick = self.PMIN + dy * i
            h_i = int(i * da)
            p.drawLine(x1 + 10, y1 - h_i, x1 + 15, y1 - h_i)
            if i % self.NMTICK == 0:
                p.drawLine(x1 + 10, y1 - h_i, x1 + 20, y1 - h_i)
                p.drawText(x1 + 24, y1 - h_i + 4, f"{ytick:.1f}")

        nl = int(math.log(self.PMAX / self.PMAXL) / 0.1 + 0.51)
        ytt = [0.0]
        for j in range(nl // 10 + 1):
            for i in range(1, 10):
                ytt.append(float(i) * (10.0 ** j))

        dla = float(self.ALTO - altol) / math.log10(self.PMAX / self.PMAXL)
        for i in range(1, min(self.NTICKL, len(ytt))):
            ytick = ytt[i]
            h_i = int(dla * math.log10(ytick / self.PMAXL) + altol)
            if ytick <= self.PMAX:
                p.drawLine(x1 + 10, y1 - h_i, x1 + 15, y1 - h_i)
                if i % self.NMTICK == 0 or i == self.NTICKL - 1 or ytick == self.PMAX:
                    p.drawLine(x1 + 10, y1 - h_i, x1 + 20, y1 - h_i)
                    p.drawText(x1 + 24, y1 - h_i + 4, f"{ytick:.1f}")


# =============================================================================
# Auxiliary Windows: Plots, Sliders, Help, About
# =============================================================================
class GrWindow(QWidget):
    """Live Pair Distribution Function g(r) window (GrFrame.java + PlotGr.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("gr", "Pair distribution function"))
        self.setFixedSize(430, 330)
        self.gr = GraphHelper()
        self.ipinta = 0
        self.x_dist = 0.0
        self.y_gmax = 0.0
        self.XMIN = 0.0
        self.XMAX = 10.0
        self.YMIN = 0.0
        self.YMAXO = 4.0
        self.YMAX = 4.0
        self.IHS = 64
        self.IVS = 30
        self.HS = 250
        self.VS = 200

    def mousePressEvent(self, event):
        self.ipinta = 1
        self.x_dist = (event.x() - self.IHS) * (self.XMAX - self.XMIN) / float(self.HS)
        self.y_gmax = self.YMAX - (event.y() - self.IVS) * (self.YMAX - self.YMIN) / float(self.VS)
        self.update()

    def mouseReleaseEvent(self, event):
        self.ipinta = 0
        self.update()

    def closeEvent(self, event):
        self.on_close()
        event.accept()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), COLOR_BP)

        dr = self.sim.drg
        ngr = self.sim.ngrm
        nmax = min(ngr, int(self.XMAX / dr + 0.5))

        x1 = [0.0]
        y1 = [0.0]
        for i in range(1, nmax + 1):
            val = self.sim.gr1[i]
            if val > 0.0:
                x1.append(i * dr)
                y1.append(val)

        if self.sim.gr1[0] > self.YMAXO:
            self.YMAX = float(int(self.sim.gr1[0] + 1.0))
        else:
            self.YMAX = self.YMAXO
        if self.YMAX > 6.0:
            self.YMAX = 6.0

        self.gr.draw_graph(
            p, self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, "g(r)", "r", 1.0
        )
        if len(x1) > 1:
            self.gr.draw_graph_line(
                p, x1, y1, 1, len(x1) - 1,
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2,
                "A-A", COLOR_A
            )

        dash_color = QColor(24, 126, 26)
        self.gr.draw_dash_line(
            p, [0.0, self.sim.rmin, self.sim.rmin], [0.0, 0.0, self.YMAX], 1, 2,
            self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, dash_color
        )
        self.gr.draw_dash_line(
            p, [0.0, self.sim.rmax, self.sim.rmax], [0.0, 0.0, self.YMAX], 1, 2,
            self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, dash_color
        )

        if self.sim.itype > 1 or self.sim.iheter == 1:
            x2 = [0.0]
            y2 = [0.0]
            for i in range(1, nmax + 1):
                val = self.sim.gr2[i]
                if val > 0.0:
                    x2.append(i * dr)
                    y2.append(val)
            if len(x2) > 1:
                self.gr.draw_graph_line(
                    p, x2, y2, 1, len(x2) - 1,
                    self.IHS, self.IVS, self.VS, self.HS,
                    self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                    self.IHS + self.HS + 10, 22 + self.IVS + self.VS // 2,
                    self.IHS + self.HS + 36, 22 + self.IVS + self.VS // 2,
                    "A-B", COLOR_B
                )

        if self.ipinta == 1:
            p.setPen(QColor(12, 65, 160))
            p.setFont(QFont("Helvetica", 11, QFont.Bold))
            p.drawText(self.IHS, (5 * self.IVS) // 2 + self.VS, f"r = {self.x_dist:.3f}")
            p.drawText(self.IHS, (6 * self.IVS) // 2 + self.VS + 4, f"g = {self.y_gmax:.3f}")


class PdxWindow(QWidget):
    """Live Horizontal Density Profile rho(x) window (PdxFrame.java + PlotPdx.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("perfdenshor", "Horizontal density profile"))
        self.setFixedSize(430, 330)
        self.gr = GraphHelper()
        self.ipinta = 0
        self.x_dist = 0.0
        self.y_gmax = 0.0
        self.XMIN = 0.0
        self.XMAX = 10.0
        self.YMIN = 0.0
        self.YMAXO = 1.0
        self.YMAX = 1.0
        self.IHS = 64
        self.IVS = 30
        self.HS = 250
        self.VS = 200

    def mousePressEvent(self, event):
        self.ipinta = 1
        self.x_dist = (event.x() - self.IHS) * (self.XMAX - self.XMIN) / float(self.HS)
        self.y_gmax = self.YMAX - (event.y() - self.IVS) * (self.YMAX - self.YMIN) / float(self.VS)
        self.update()

    def mouseReleaseEvent(self, event):
        self.ipinta = 0
        self.update()

    def closeEvent(self, event):
        self.on_close()
        event.accept()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), QColor(246, 250, 240))

        dr = self.sim.drgx
        ngr = self.sim.ngrm
        nmax = min(ngr, int(self.XMAX / dr + 0.5)) if dr > 0 else 0

        if self.sim.gw1[0] > self.YMAXO:
            self.YMAX = float(int(self.sim.gw1[0] + 1.0))
        else:
            self.YMAX = self.YMAXO

        self.gr.draw_graph(
            p, self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, "r(x)", "x", 0.0
        )

        x1, y1 = [0.0], [0.0]
        arr1 = self.sim.gw1 if self.sim.iheter == 0 else self.sim.gw2
        for i in range(1, nmax + 1):
            if arr1[i] > 0.0:
                x1.append(i * dr)
                y1.append(arr1[i])
        col1 = COLOR_A if self.sim.iheter == 0 else COLOR_B
        if len(x1) > 1:
            self.gr.draw_graph_line(
                p, x1, y1, 1, len(x1) - 1,
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2,
                "P-A", col1
            )

        if self.sim.itype > 1 or self.sim.iheter == 1:
            x2, y2 = [0.0], [0.0]
            arr2 = self.sim.gw2 if self.sim.iheter == 0 else self.sim.gw3
            for i in range(1, nmax + 1):
                if arr2[i] > 0.0:
                    x2.append(i * dr)
                    y2.append(arr2[i])
            col2 = COLOR_C if self.sim.iheter == 1 else COLOR_B
            if len(x2) > 1:
                self.gr.draw_graph_line(
                    p, x2, y2, 1, len(x2) - 1,
                    self.IHS, self.IVS, self.VS, self.HS,
                    self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                    self.IHS + self.HS + 10, 22 + self.IVS + self.VS // 2,
                    self.IHS + self.HS + 36, 22 + self.IVS + self.VS // 2,
                    "P-B", col2
                )

        if self.sim.itype > 2 and self.sim.iheter == 0:
            x3, y3 = [0.0], [0.0]
            for i in range(1, nmax + 1):
                if self.sim.gw3[i] > 0.0:
                    x3.append(i * dr)
                    y3.append(self.sim.gw3[i])
            if len(x3) > 1:
                self.gr.draw_graph_line(
                    p, x3, y3, 1, len(x3) - 1,
                    self.IHS, self.IVS, self.VS, self.HS,
                    self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                    self.IHS + self.HS + 10, 44 + self.IVS + self.VS // 2,
                    self.IHS + self.HS + 36, 44 + self.IVS + self.VS // 2,
                    "P-C", COLOR_C
                )

        if self.ipinta == 1:
            p.setPen(QColor(12, 65, 160))
            p.setFont(QFont("Helvetica", 11, QFont.Bold))
            p.drawText(self.IHS, (5 * self.IVS) // 2 + self.VS, f"x = {self.x_dist:.3f}")
            p.drawText(self.IHS, (6 * self.IVS) // 2 + self.VS + 4, f"r = {self.y_gmax:.3f}")


class PdyWindow(QWidget):
    """Live Vertical Density Profile rho(y) window (PdyFrame.java + PlotPdy.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("perfdensver", "Vertical density profile"))
        self.setFixedSize(430, 330)
        self.gr = GraphHelper()
        self.ipinta = 0
        self.x_dist = 0.0
        self.y_gmax = 0.0
        self.XMIN = 0.0
        self.XMAX = 5.0
        self.YMIN = 0.0
        self.YMAXO = 1.0
        self.YMAX = 1.0
        self.IHS = 64
        self.IVS = 30
        self.HS = 250
        self.VS = 200

    def mousePressEvent(self, event):
        self.ipinta = 1
        self.x_dist = (event.x() - self.IHS) * (self.XMAX - self.XMIN) / float(self.HS)
        self.y_gmax = self.YMAX - (event.y() - self.IVS) * (self.YMAX - self.YMIN) / float(self.VS)
        self.update()

    def mouseReleaseEvent(self, event):
        self.ipinta = 0
        self.update()

    def closeEvent(self, event):
        self.on_close()
        event.accept()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), COLOR_BP)

        self.XMAX = 1.0 / self.sim.rsize if self.sim.rsize > 0 else 5.0
        dr = self.sim.drgy
        nmax = self.sim.ngrmy

        x1, y1 = [0.0], [0.0]
        for i in range(1, nmax + 1):
            val = self.sim.gwy[i]
            if val > 0.0:
                x1.append(self.XMAX - (i - 1) * dr)
                y1.append(val)

        if self.sim.gwy[0] > self.YMAXO:
            self.YMAX = float(int(self.sim.gwy[0] + 1.0))
        else:
            self.YMAX = self.YMAXO

        self.gr.draw_graph(
            p, self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, "r(y)", "y", 0.0
        )
        if len(x1) > 1:
            self.gr.draw_graph_line(
                p, x1, y1, 1, len(x1) - 1,
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2,
                "P-X", QColor(15, 50, 200)
            )

        if self.ipinta == 1:
            p.setPen(QColor(12, 65, 160))
            p.setFont(QFont("Helvetica", 11, QFont.Bold))
            p.drawText(self.IHS, (5 * self.IVS) // 2 + self.VS, f"y = {self.x_dist:.3f}")
            p.drawText(self.IHS, (6 * self.IVS) // 2 + self.VS + 4, f"r = {self.y_gmax:.3f}")


class PhaseDiagramWindow(QWidget):
    """Phase Diagram window (PhD.java + PlotPD.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("diagramafases", "Phase diagram"))
        self.PHSIZE = 390
        self.PVSIZE = 390
        self.setFixedSize(self.PHSIZE, self.PVSIZE)
        self.HS = (68 * self.PHSIZE) // 100
        self.VS = (68 * self.PVSIZE) // 100
        self.IHS = 64
        self.IVS = 30
        self.XMIN = 0.0
        self.XMAX = 1.0
        self.YMIN = 0.2
        self.YMAX = 0.7
        self.TEMP = sim.temperatura
        self.DENS = sim.densidad
        self.ipinta = 0
        self.x_dens = 0.0
        self.y_temp = 0.0
        self.gr = GraphHelper()

    def ptransient(self, temp: float, dens: float):
        self.TEMP = temp
        self.DENS = dens
        self.update()

    def mousePressEvent(self, event):
        self.ipinta = 1
        self.x_dens = (event.x() - self.IHS) * (self.XMAX - self.XMIN) / float(self.HS)
        self.y_temp = self.YMAX - (event.y() - self.IVS) * (self.YMAX - self.YMIN) / float(self.VS)
        self.update()

    def mouseReleaseEvent(self, event):
        self.ipinta = 0
        self.update()

    def closeEvent(self, event):
        self.on_close()
        event.accept()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), QColor(244, 250, 232))

        itype = self.sim.itype
        ihs, ivs, vs, hs = self.IHS, self.IVS, self.VS, self.HS
        xmin, xmax, ymin, ymax = self.XMIN, self.XMAX, self.YMIN, self.YMAX

        c_gas_solid = QColor(216, 255, 110)
        c_mix = QColor(191, 207, 218)
        c_coex = QColor(202, 218, 97)

        if itype == 1:
            self.gr.draw_graph_poly(p, pd_data.rho1a, pd_data.temp1a, 0, len(pd_data.rho1a) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_gas_solid)
            self.gr.draw_graph_poly(p, pd_data.rho1c, pd_data.temp1c, 0, len(pd_data.rho1c) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_gas_solid)
            self.gr.draw_graph_poly(p, pd_data.rho1d, pd_data.temp1d, 0, len(pd_data.rho1d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_coex)
            self.gr.draw_graph_line(p, pd_data.rho1d, pd_data.temp1d, 0, len(pd_data.rho1d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
            self.gr.draw_graph_line(p, pd_data.rt1, pd_data.tt, 0, len(pd_data.rt1) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
        elif itype == 2:
            self.gr.draw_graph_poly(p, pd_data.rho2a, pd_data.temp2a, 0, len(pd_data.rho2a) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_gas_solid)
            self.gr.draw_graph_poly(p, pd_data.rho2b, pd_data.temp2b, 0, len(pd_data.rho2b) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_mix)
            self.gr.draw_graph_poly(p, pd_data.rho2c, pd_data.temp2c, 0, len(pd_data.rho2c) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_mix)
            self.gr.draw_graph_poly(p, pd_data.rho2d, pd_data.temp2d, 0, len(pd_data.rho2d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_coex)
            self.gr.draw_graph_line(p, pd_data.rho2b, pd_data.temp2b, 0, len(pd_data.rho2b) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
            self.gr.draw_graph_line(p, pd_data.rho2d, pd_data.temp2d, 0, len(pd_data.rho2d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
            self.gr.draw_graph_line(p, pd_data.rt2, pd_data.tt, 0, len(pd_data.rt2) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
        elif itype == 3:
            self.gr.draw_graph_poly(p, pd_data.rho3a, pd_data.temp3a, 0, len(pd_data.rho3a) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_gas_solid)
            self.gr.draw_graph_poly(p, pd_data.rho3b, pd_data.temp3b, 0, len(pd_data.rho3b) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_mix)
            self.gr.draw_graph_poly(p, pd_data.rho3c, pd_data.temp3c, 0, len(pd_data.rho3c) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_mix)
            self.gr.draw_graph_poly(p, pd_data.rho3d, pd_data.temp3d, 0, len(pd_data.rho3d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, c_coex)
            self.gr.draw_graph_line(p, pd_data.rho3d, pd_data.temp3d, 0, len(pd_data.rho3d) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
            self.gr.draw_graph_line(p, pd_data.rho3b, pd_data.temp3b, 0, len(pd_data.rho3b) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)
            self.gr.draw_graph_line(p, pd_data.rt3, pd_data.tt, 0, len(pd_data.rt3) - 1, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, 0, 0, 0, 0, "", COLOR_TEXT)

        self.gr.draw_graph(
            p, ihs, ivs, vs, hs, xmin, xmax, ymin, ymax,
            "T", self.sim.messages.get("densidad", "Density"), ymin
        )

        if self.sim.istart > 0 and self.sim.istate > 0:
            idx = self.sim.istate - 1
            self.gr.draw_graph_point(
                p, self.sim.rstate[idx], self.sim.tstate[idx],
                ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, QColor(220, 20, 20), filled=True
            )
        self.gr.draw_graph_point(
            p, self.DENS, self.TEMP,
            ihs, ivs, vs, hs, xmin, xmax, ymin, ymax, QColor(220, 20, 20), filled=False
        )

        p.setPen(COLOR_TEXT)
        p.setFont(QFont("Courier", 13, QFont.Bold))
        if itype == 1:
            p.drawText(hs // 2 + ihs - ihs // 4, vs // 4 + ivs, "G")
            p.drawText(hs // 2 + 2 * ihs, vs // 2 + ivs + ivs // 3 + ivs // 6, "L")
            p.drawText(hs // 4, vs // 2 + ivs + ivs // 3, "V")
            p.drawText(hs + ihs - ihs // 3, vs // 2 + ivs + ivs // 2, "S")
            p.drawText(hs // 3 + ihs // 5 + ihs, vs // 2 + ihs, "L+V")
            p.drawText(hs // 4 + (2 * ihs) // 3 + ihs, vs // 2 + 4 * ivs - ivs // 5, "S+V")
        elif itype == 2:
            p.drawText(hs // 4 + ihs, vs // 4 - ivs // 6 + ivs, "G")
            p.drawText(hs // 2 + ihs + ihs // 5, vs // 4 + ivs + ivs // 6, "A+B")
            p.drawText(hs // 2 + 2 * ihs + ihs // 12, vs // 2 + ivs // 2 + ivs + ivs // 10, "L")
            p.drawText(ihs + ihs // 7, ivs + vs // 2 + ivs // 2, "V")
            p.drawText(hs + ivs, vs // 2 + ihs, "S")
            p.drawText(hs // 2 - ihs // 2 + ihs, vs // 2 + ivs + ivs // 3, "L+V")
            p.drawText(hs // 2 + ihs // 2, vs // 2 + 2 * ihs + 9, "S+V")
        elif itype == 3:
            p.drawText(hs // 4 - ihs // 15 + ihs, vs // 4 + ivs - ivs // 10, "G")
            p.drawText(hs // 2 + ihs + ihs // 6, vs // 2 - ihs // 2, "A+B+C")
            p.drawText(hs // 2 + 2 * ihs + ihs // 13, vs // 2 + ivs + ivs // 2, "L")
            p.drawText(ihs + 4, vs // 2 + ihs - 9, "V")
            p.drawText(hs + ivs, vs // 2 + ihs, "S")
            p.drawText(hs // 3 + ihs + ihs // 5, vs // 2 + ivs + ivs // 2, "L+V")
            p.drawText(hs // 2 + ihs // 2, vs // 2 + 2 * ihs + 9, "S+V")

        if self.ipinta == 1:
            p.setPen(QColor(12, 65, 160))
            p.setFont(QFont("Helvetica", 11, QFont.Bold))
            p.drawText(ihs, 2 * ivs + vs + 4, f"Dens = {self.x_dens:.3f}")
            p.drawText(ihs, (5 * ivs) // 2 + vs + 8, f"Temp = {self.y_temp:.3f}")


class PlotAtomAtomCanvas(QWidget):
    """Canvas for Atom-Atom LJ potential plot (PlotPanel.java)."""

    def __init__(self, sim):
        super().__init__()
        self.sim = sim
        self.ircaa = int(sim.rcaa * 100 + 0.51)
        self.ircab = int(sim.rcab * 100 + 0.51)
        self.dr = 0.01
        self.XMIN = 1.0
        self.XMAX = 2.0
        self.YMIN = -1.0
        self.YMAX = 1.0
        self.IHS = 64
        self.IVS = 40
        self.HS = 250
        self.VS = 195
        self.gr = GraphHelper()
        self.setMinimumSize(400, 295)

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), COLOR_BP)

        rcaa = self.ircaa * self.dr
        rcab = self.ircab * self.dr
        uminaa = 4.0 * (rcaa ** -12 - rcaa ** -6)
        uminab = 4.0 * (rcab ** -12 - rcab ** -6)

        nmin = int(self.XMIN / self.dr + 0.51)
        nmax = int(self.XMAX / self.dr + 0.51)

        self.gr.draw_graph(
            p, self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX, "u(r)", "r", 0.0
        )

        xx = np.arange(0, 255) * self.dr
        yy_aa = np.zeros_like(xx)
        for i in range(nmin, self.ircaa + 1):
            yy_aa[i] = 4.0 * (xx[i] ** -12 - xx[i] ** -6) - uminaa

        p.setPen(COLOR_TEXT)
        p.setFont(QFont("Helvetica", 11, QFont.Bold))
        p.drawText(20, self.height() - 8, f"{self.sim.messages.get('alcanceaa', 'Cutoff A-A')} = {rcaa:.2f}")
        self.gr.draw_graph_line(
            p, xx, yy_aa, nmin, min(self.ircaa, nmax),
            self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, self.YMIN, self.YMAX,
            self.IHS + self.HS + 10, self.IVS + self.VS // 2,
            self.IHS + self.HS + 36, self.IVS + self.VS // 2,
            "A-A", COLOR_A
        )

        if self.sim.itype > 1 or self.sim.iheter == 1:
            p.setPen(COLOR_TEXT)
            p.setFont(QFont("Helvetica", 11, QFont.Bold))
            p.drawText(20, 22, f"{self.sim.messages.get('alcanceab', 'Cutoff A-B')} = {rcab:.2f}")
            yy_ab = np.zeros_like(xx)
            for i in range(nmin, self.ircab + 1):
                yy_ab[i] = 4.0 * (xx[i] ** -12 - xx[i] ** -6) - uminab
            self.gr.draw_graph_line(
                p, xx, yy_ab, nmin, min(self.ircab, nmax),
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, self.YMIN, self.YMAX,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2 + 22,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2 + 22,
                "A-B", COLOR_B
            )


class InteraccionWindow(QWidget):
    """Atom-Atom interaction potential window (Interaccion.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("interacciones", "Interactions"))
        self.setFixedSize(460, 440)

        layout = QVBoxLayout(self)
        self.canvas = PlotAtomAtomCanvas(sim)

        self.rcab_slider = None
        if sim.itype > 1 or sim.iheter == 1:
            self.rcab_slider = LabeledSlider(
                Qt.Horizontal, 100, 250, int(sim.rcab * 100 + 0.51),
                major_step=50, minor_step=10, scale_factor=0.01, decimals=1
            )
            self.rcab_slider.valueChanged.connect(self._on_rcab)
            layout.addWidget(self.rcab_slider)

        layout.addWidget(self.canvas, 1)

        self.rcaa_slider = LabeledSlider(
            Qt.Horizontal, 100, 250, int(sim.rcaa * 100 + 0.51),
            major_step=50, minor_step=10, scale_factor=0.01, decimals=1
        )
        self.rcaa_slider.valueChanged.connect(self._on_rcaa)
        layout.addWidget(self.rcaa_slider)

        self.update_enabled()

    def _on_rcaa(self, val):
        self.canvas.ircaa = val
        self.sim.rcaa = val * 0.01
        self.canvas.update()

    def _on_rcab(self, val):
        self.canvas.ircab = val
        self.sim.rcab = val * 0.01
        self.canvas.update()

    def update_enabled(self):
        enabled = not (self.sim.istart != 0 and self.sim.ipause != 1)
        self.rcaa_slider.setEnabled(enabled)
        if self.rcab_slider is not None:
            self.rcab_slider.setEnabled(enabled)

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class PlotWallCanvas(QWidget):
    """Canvas for Wall-Atom 10-4 potential plot (PlotPanelW.java)."""

    def __init__(self, sim):
        super().__init__()
        self.sim = sim
        self.ircaa = int(sim.rw1 * 100 + 0.51)
        self.ircab = int(sim.rw2 * 100 + 0.51)
        self.ircac = int(sim.rw3 * 100 + 0.51)
        self.eps = sim.epsw
        self.dr = 0.01
        self.XMIN = 1.0
        self.XMAX = 5.0
        self.YMIN = -1.0
        self.YMAX = 1.0
        self.IHS = 64
        self.IVS = 30
        self.HS = 250
        self.VS = 185
        self.gr = GraphHelper()
        self.setMinimumSize(410, 260)

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        p.fillRect(self.rect(), COLOR_BP)

        rcaa = self.ircaa * self.dr
        rcab = self.ircab * self.dr
        rcac = self.ircac * self.dr
        eps = self.eps

        uminaa = 3.07 * eps * (rcaa ** -10 - rcaa ** -4)
        uminab = 3.07 * eps * (rcab ** -10 - rcab ** -4)
        uminac = 3.07 * eps * (rcac ** -10 - rcac ** -4)

        ymin = float(int(eps * self.YMIN - 0.51))
        ymax = float(int(eps * self.YMAX + 0.51))
        nmin = int(self.XMIN / self.dr + 0.51)
        nmax = int(self.XMAX / self.dr + 0.51)

        self.gr.draw_graph(
            p, self.IHS, self.IVS, self.VS, self.HS,
            self.XMIN, self.XMAX, ymin, ymax, "w(z)", "z", 0.0
        )

        xx = np.arange(0, 505) * self.dr
        if self.sim.iheter == 0:
            yy_a = np.zeros_like(xx)
            for i in range(nmin, self.ircaa + 1):
                yy_a[i] = 3.07 * eps * (xx[i] ** -10 - xx[i] ** -4) - uminaa
            self.gr.draw_graph_line(
                p, xx, yy_a, nmin, min(self.ircaa, nmax),
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, ymin, ymax,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2,
                "P-A", COLOR_A
            )

        if self.sim.itype > 1 or self.sim.iheter == 1:
            yy_b = np.zeros_like(xx)
            for i in range(nmin, self.ircab + 1):
                yy_b[i] = 3.07 * eps * (xx[i] ** -10 - xx[i] ** -4) - uminab
            self.gr.draw_graph_line(
                p, xx, yy_b, nmin, min(self.ircab, nmax),
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, ymin, ymax,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2 + 22,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2 + 22,
                "P-B", COLOR_B
            )

        if self.sim.itype > 2 or self.sim.iheter == 1:
            yy_c = np.zeros_like(xx)
            for i in range(nmin, self.ircac + 1):
                yy_c[i] = 3.07 * eps * (xx[i] ** -10 - xx[i] ** -4) - uminac
            self.gr.draw_graph_line(
                p, xx, yy_c, nmin, min(self.ircac, nmax),
                self.IHS, self.IVS, self.VS, self.HS,
                self.XMIN, self.XMAX, ymin, ymax,
                self.IHS + self.HS + 10, self.IVS + self.VS // 2 + 44,
                self.IHS + self.HS + 36, self.IVS + self.VS // 2 + 44,
                "P-C", COLOR_C
            )


class InteraccionWallWindow(QWidget):
    """Wall-Atom interaction potential window (InteraccionW.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(
            f"{sim.messages.get('interacciones', 'Interactions')} {sim.messages.get('atomopared', 'Wall-Atom')}"
        )
        self.setFixedWidth(480)

        layout = QVBoxLayout(self)
        self.canvas = PlotWallCanvas(sim)
        layout.addWidget(self.canvas, 1)

        grid = QGridLayout()
        self.ep_slider = LabeledSlider(
            Qt.Horizontal, 100, 500, int(sim.epsw * 100 + 0.51),
            major_step=50, minor_step=10, scale_factor=0.01, decimals=1
        )
        self.ep_slider.valueChanged.connect(self._on_eps)
        lbl_ep = QLabel(sim.messages.get("profundidad", "Well depth"))
        lbl_ep.setFont(QFont("Helvetica", 10, QFont.Bold))
        grid.addWidget(lbl_ep, 0, 0)
        grid.addWidget(self.ep_slider, 0, 1)

        self.rcaa_slider = None
        if sim.iheter == 0:
            self.rcaa_slider = LabeledSlider(
                Qt.Horizontal, 100, 500, int(sim.rw1 * 100 + 0.51),
                major_step=50, minor_step=10, scale_factor=0.01, decimals=1
            )
            self.rcaa_slider.valueChanged.connect(self._on_rw1)
            lbl_a = QLabel(f"{sim.messages.get('alcancep', 'Cutoff Wall')}-A")
            lbl_a.setFont(QFont("Helvetica", 10, QFont.Bold))
            grid.addWidget(lbl_a, 1, 0)
            grid.addWidget(self.rcaa_slider, 1, 1)

        self.rcab_slider = None
        if sim.itype > 1 or sim.iheter == 1:
            self.rcab_slider = LabeledSlider(
                Qt.Horizontal, 100, 500, int(sim.rw2 * 100 + 0.51),
                major_step=50, minor_step=10, scale_factor=0.01, decimals=1
            )
            self.rcab_slider.valueChanged.connect(self._on_rw2)
            lbl_b = QLabel(f"{sim.messages.get('alcancep', 'Cutoff Wall')}-B")
            lbl_b.setFont(QFont("Helvetica", 10, QFont.Bold))
            grid.addWidget(lbl_b, 2, 0)
            grid.addWidget(self.rcab_slider, 2, 1)

        self.rcac_slider = None
        if sim.itype > 2 or sim.iheter == 1:
            self.rcac_slider = LabeledSlider(
                Qt.Horizontal, 100, 500, int(sim.rw3 * 100 + 0.51),
                major_step=50, minor_step=10, scale_factor=0.01, decimals=1
            )
            self.rcac_slider.valueChanged.connect(self._on_rw3)
            lbl_c = QLabel(f"{sim.messages.get('alcancep', 'Cutoff Wall')}-C")
            lbl_c.setFont(QFont("Helvetica", 10, QFont.Bold))
            grid.addWidget(lbl_c, 3, 0)
            grid.addWidget(self.rcac_slider, 3, 1)

        layout.addLayout(grid)
        self.update_enabled()

    def _on_eps(self, val):
        self.canvas.eps = val * 0.01
        self.sim.epsw = val * 0.01
        self.canvas.update()

    def _on_rw1(self, val):
        self.canvas.ircaa = val
        self.sim.rw1 = val * 0.01
        self.canvas.update()

    def _on_rw2(self, val):
        self.canvas.ircab = val
        self.sim.rw2 = val * 0.01
        self.canvas.update()

    def _on_rw3(self, val):
        self.canvas.ircac = val
        self.sim.rw3 = val * 0.01
        self.canvas.update()

    def update_enabled(self):
        enabled = not (self.sim.istart != 0 and self.sim.ipause != 1)
        self.ep_slider.setEnabled(enabled)
        if self.rcaa_slider is not None:
            self.rcaa_slider.setEnabled(enabled)
        if self.rcab_slider is not None:
            self.rcab_slider.setEnabled(enabled)
        if self.rcac_slider is not None:
            self.rcac_slider.setEnabled(enabled)

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class ComposicionWindow(QWidget):
    """Mixture mole fraction sliders window (Composicion.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("mole", "Mole fractions"))

        grid = QGridLayout(self)
        if sim.itype == 2:
            ixma = (100 * sim.natcm[1]) // max(1, sim.natcm[2])
            ixmb = 50
        else:
            ixma = (100 * sim.natcm[1]) // max(1, sim.natcm[3])
            ixmb = (100 * (sim.natcm[2] - sim.natcm[1])) // max(1, sim.natcm[3])

        lbl_a = QLabel(sim.messages.get("moleA", "Mole fraction of A"))
        lbl_a.setFont(QFont("Helvetica", 11, QFont.Bold))
        self.xmola_slider = LabeledSlider(
            Qt.Horizontal, 0, 100, ixma,
            major_step=25, minor_step=5, scale_factor=0.01, decimals=2
        )
        self.xmola_slider.valueChanged.connect(self._on_xmola)
        grid.addWidget(lbl_a, 0, 0)
        grid.addWidget(self.xmola_slider, 0, 1)

        self.xmolb_slider = None
        if sim.itype > 2:
            lbl_b = QLabel(sim.messages.get("moleB", "Mole fraction of B"))
            lbl_b.setFont(QFont("Helvetica", 11, QFont.Bold))
            self.xmolb_slider = LabeledSlider(
                Qt.Horizontal, 0, 100, ixmb,
                major_step=25, minor_step=5, scale_factor=0.01, decimals=2
            )
            self.xmolb_slider.valueChanged.connect(self._on_xmolb)
            grid.addWidget(lbl_b, 1, 0)
            grid.addWidget(self.xmolb_slider, 1, 1)

        self.setFixedSize(540, 110 if sim.itype == 2 else 175)
        self.update_enabled()

    def _on_xmola(self, val):
        self.sim.set_nparta(val * 0.01)

    def _on_xmolb(self, val):
        self.sim.set_npartb(val * 0.01)

    def update_enabled(self):
        enabled = not (self.sim.istart != 0 and self.sim.ipause != 1)
        self.xmola_slider.setEnabled(enabled)
        if self.xmolb_slider is not None:
            self.xmolb_slider.setEnabled(enabled)

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class ElongacionWindow(QWidget):
    """Diatomic bond length / elongation window (Elongacion.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("elong", "Elongation"))
        self.setFixedSize(360, 115)

        layout = QVBoxLayout(self)
        self.lbl = QLabel(f"{sim.messages.get('elong', 'Elongation')} = {sim.elon:.2f}")
        self.lbl.setFont(QFont("Helvetica", 11, QFont.Bold))
        self.lbl.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.lbl)

        self.slider = LabeledSlider(
            Qt.Horizontal, 20, 100, int(sim.elon * 100 + 0.5),
            major_step=20, minor_step=5, scale_factor=0.01, decimals=1
        )
        self.slider.valueChanged.connect(self._on_change)
        layout.addWidget(self.slider)
        self.update_enabled()

    def _on_change(self, val):
        self.sim.set_elon(val * 0.01)
        self.lbl.setText(f"{self.sim.messages.get('elong', 'Elongation')} = {self.sim.elon:.2f}")

    def update_enabled(self):
        enabled = not (self.sim.istart != 0 and self.sim.ipause != 1)
        self.slider.setEnabled(enabled)

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class SetNpartWindow(QWidget):
    """Particle number slider window (SetNpart.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("numeroparticulas", "Particle number"))
        self.setFixedSize(360, 115)

        layout = QVBoxLayout(self)
        self.lbl = QLabel(f"{sim.messages.get('numeroparticulas', 'Particle number')}: {sim.NMOL}")
        self.lbl.setFont(QFont("Helvetica", 11, QFont.Bold))
        self.lbl.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.lbl)

        self.slider = LabeledSlider(
            Qt.Horizontal, sim.NMOLMIN, sim.NMOLMAX, sim.NMOL,
            major_step=sim.NMSTP, minor_step=sim.NMSTP, scale_factor=1.0, decimals=0
        )
        self.slider.setSingleStep(sim.NMSTP)
        self.slider.setPageStep(sim.NMSTP)
        self.slider.valueChanged.connect(self._on_change)
        layout.addWidget(self.slider)
        self.update_enabled()

    def _on_change(self, val):
        if self.sim.istart != 0 and self.sim.ipause != 1:
            return
        snapped = (val // 6) * 6
        self.sim.NMOLN = max(self.sim.NMOLMIN, snapped)
        self.sim.icn = 1
        self.lbl.setText(f"{self.sim.messages.get('numeroparticulas', 'Particle number')}: {self.sim.NMOLN}")

    def update_enabled(self):
        enabled = not (self.sim.istart != 0 and self.sim.ipause != 1)
        self.slider.setEnabled(enabled)

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class SetVelocWindow(QWidget):
    """Visualization delay slider window (SetVeloc.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("sincrot", "Synchrony"))
        self.setFixedSize(360, 115)

        layout = QVBoxLayout(self)
        self.lbl = QLabel(f"{sim.messages.get('sincrotime', 'Visual synchronization time (ms)')}: {sim.NSLEEP}")
        self.lbl.setFont(QFont("Helvetica", 10, QFont.Bold))
        self.lbl.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.lbl)

        self.slider = LabeledSlider(
            Qt.Horizontal, 0, 400, sim.NSLEEP,
            major_step=100, minor_step=25, scale_factor=1.0, decimals=0
        )
        self.slider.valueChanged.connect(self._on_change)
        layout.addWidget(self.slider)

    def _on_change(self, val):
        self.sim.NSLEEP = val
        self.sim.timer.setInterval(max(1, val))
        self.lbl.setText(f"{self.sim.messages.get('sincrotime', 'Visual synchronization time (ms)')}: {val}")

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class HelpWindow(QWidget):
    """HTML Basic Help browser (MetalworksHelp.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("ayudabasica", "Basic Help"))
        self.resize(470, 580)

        layout = QVBoxLayout(self)
        self.browser = QTextBrowser()
        self.browser.setStyleSheet("background-color: white; color: black;")
        self.browser.setOpenExternalLinks(True)
        fichero = sim.messages.get("fichero", "HelpFiles/Guion000.html")
        full_path = resource_path(fichero)
        self.browser.setSearchPaths([os.path.dirname(full_path), BASE_DIR])
        self.browser.setSource(QUrl.fromLocalFile(full_path))

        self.browser.setContextMenuPolicy(Qt.CustomContextMenu)
        self.browser.customContextMenuRequested.connect(self._show_popup)
        layout.addWidget(self.browser)

    def _show_popup(self, pos):
        menu = QMenu(self)
        act_home = menu.addAction(self.sim.messages.get("home", "Top"))
        act_back = menu.addAction(self.sim.messages.get("atras", "Back"))
        chosen = menu.exec_(self.browser.mapToGlobal(pos))
        if chosen == act_home:
            self.browser.home()
        elif chosen == act_back:
            self.browser.backward()

    def closeEvent(self, event):
        self.on_close()
        event.accept()


class AboutWindow(QWidget):
    """About DiMol2D window (Identf.java)."""

    def __init__(self, sim, on_close):
        super().__init__()
        self.sim = sim
        self.on_close = on_close
        self.setWindowTitle(sim.messages.get("acerca", "About DiMol2D"))
        self.setFixedSize(570, 200)
        self.dm2ds = QPixmap(resource_path("Image/DM2D1-1s.png"))
        self.csic = QPixmap(resource_path("Icon/csic_trans.png"))
        self.qft = QPixmap(resource_path("Icon/qft_t100.png"))

    def paintEvent(self, event):
        p = QPainter(self)
        p.fillRect(self.rect(), COLOR_BAR_BG)
        if not self.csic.isNull():
            p.drawPixmap(10, (self.height() - self.csic.height()) // 2, self.csic)
        if not self.qft.isNull():
            p.drawPixmap(self.width() - self.qft.width() - 10, (self.height() - self.qft.height()) // 2, self.qft)

        x_off = 90
        if not self.dm2ds.isNull():
            p.drawPixmap(x_off + 60, -35, self.dm2ds)

        yor = self.height() // 3 + 10
        p.setPen(QColor(255, 245, 130))
        p.setFont(QFont("Helvetica", 11, QFont.Bold))
        p.drawText(x_off, yor, f"{self.sim.messages.get('design', 'A 2D Molecular Dynamics program designed by')}:")
        p.drawText(x_off, yor + 22, "E. Lomba, N.G. Almarza, C. Martín.")
        p.setFont(QFont("Helvetica", 9, QFont.Bold))
        p.drawText(x_off + 60, yor + 60, "IQFR, CSIC, Madrid (2004-2008) / Python+Numba port.")
        p.drawText(x_off + 60, yor + 76, self.sim.messages.get("bug", "Report bugs to E.Lomba@iqfr.csic.es"))
        p.drawText(x_off + 60, yor + 92, "http://www.qft.iqfr.csic.es/DiMol2D")

    def closeEvent(self, event):
        self.on_close()
        event.accept()


# =============================================================================
# Core Simulation Canvas & Controller (Simulation.java)
# =============================================================================
class SimulationWidget(QWidget):
    """Main 2D simulation canvas, MD loop timer, and right-click popup menu."""

    def __init__(self, main_win, messages: dict, rhsize: int, rvsize: int):
        super().__init__()
        self.main_win = main_win
        self.messages = messages
        self.engine = MDEngine()

        self.RHSIZE = rhsize
        self.RVSIZE = rvsize
        self.HSIZE = (80 * self.RHSIZE) // 100
        self.VSIZE = self.HSIZE
        self.SIZE = 20
        self.setFixedSize(self.RHSIZE, self.RVSIZE)

        # Simulation parameters & state
        self.NMOL = 450
        self.NMOLMIN = 150
        self.NMOLMAX = 1050
        self.NMSTP = 150
        self.NMOLN = 450
        self.icn = 0

        self.NGRMAX = 5000
        self.NGRMAXY = 5000
        self.ngrm = 800
        self.ngrmy = 1600

        self.pres = 0.0
        self.temperatura = 0.0
        self.densidad = 0.0
        self.densidad_o = 0.0
        self.rcaa = 2.5
        self.rcab = 1.122462
        self.tistep = 0.005
        self.scaflg = 1.00
        self.ifrsca = 1
        self.ncp = 10

        self.xmola = 1.0
        self.xmolb = 0.0
        self.xmolc = 0.0

        self.rmin = 0.95
        self.rmax = 1.35
        self.drg = 0.02
        self.drgx = 0.02
        self.drgy = 0.02
        self.rsize = math.sqrt(0.5 / self.NMOL)
        self.rimin = (self.rmin * self.rsize) ** 2
        self.rimax = (self.rmax * self.rsize) ** 2

        self.ipmol = 1
        self.ipbon = 0
        self.iheter = 0
        self.itype = 1
        self.itype_old = 1
        self.idum = 1
        self.idum_old = 2
        self.elon = 1.0

        self.iwall = 0
        self.iwally = 0
        self.igrav = 0
        self.rw1 = 4.165
        self.rw2 = 4.165
        self.rw3 = 1.165
        self.rwy = 1.165
        self.epsw = 2.00
        self.fgy0 = -0.02

        self.itset = 0
        self.irhoset = 0
        self.irho = 0
        self.IRHOINIT = 0
        self.icrho = 0
        self.icstate = 0
        self.inic = 0
        self.inicp = 1
        self.istart = 0
        self.ipause = 0
        self.ib = 0
        self.istate = 0
        self.NSLEEP = 25
        self.indexrun = 0
        self.indexrun1 = 0

        # Arrays (1-based indexing for particle coordinates to match Java/Fortran)
        self.ix = np.zeros(self.NMOLMAX + 2, dtype=np.int32)
        self.iy = np.zeros(self.NMOLMAX + 2, dtype=np.int32)
        self.ax = np.zeros(self.NMOLMAX + 2, dtype=np.float64)
        self.ay = np.zeros(self.NMOLMAX + 2, dtype=np.float64)
        self.gr1 = np.zeros(self.NGRMAX, dtype=np.float64)
        self.gr2 = np.zeros(self.NGRMAX, dtype=np.float64)
        self.gw1 = np.zeros(self.NGRMAX, dtype=np.float64)
        self.gw2 = np.zeros(self.NGRMAX, dtype=np.float64)
        self.gw3 = np.zeros(self.NGRMAX, dtype=np.float64)
        self.gwy = np.zeros(self.NGRMAXY, dtype=np.float64)
        self.tstate = np.zeros(1000, dtype=np.float64)
        self.rstate = np.zeros(1000, dtype=np.float64)
        self.natcm = np.zeros(10, dtype=np.int32)
        self.natcm_old = np.zeros(10, dtype=np.int32)
        self.natcm[1] = self.NMOL

        # Load images
        self.img_bg = QPixmap(resource_path("Image/drops21.jpg"))
        self.img_dm2d = QPixmap(resource_path("Image/DM2D1-1.png"))
        self.img_contador = QPixmap(resource_path("Image/contador3.gif"))
        self.img_digits = [QPixmap(resource_path(f"Image/{d}.gif")) for d in range(10)]

        # Simulation step timer
        self.timer = QTimer(self)
        self.timer.setInterval(self.NSLEEP)
        self.timer.timeout.connect(self._step_simulation)

        # Idle banner animation timer when paused or not yet started
        self.banner_timer = QTimer(self)
        self.banner_timer.setInterval(45)
        self.banner_timer.timeout.connect(self._tick_banner)
        self.banner_timer.start()

    def _tick_banner(self):
        if self.istart == 0 or self.ipause == 1:
            self.indexrun = (self.indexrun + 2) % max(1, self.RVSIZE)
            self.update()

    def contextMenuEvent(self, event):
        menu = QMenu(self)
        title_act = menu.addAction(self.messages.get("control", "Control Menu"))
        title_act.setEnabled(False)
        menu.addSeparator()

        lbl_start = self.messages.get("inicios", "Running") if (self.istart == 1 and self.ipause == 0) else self.messages.get("inicio", "Start")
        lbl_pause = self.messages.get("parado", "Stopped") if self.ipause == 1 else self.messages.get("pausa", "Pause")
        lbl_cont = self.messages.get("sigue", "Go on")
        lbl_end = self.messages.get("fin", "End")

        act_start = menu.addAction(lbl_start)
        act_pause = menu.addAction(lbl_pause)
        act_cont = menu.addAction(lbl_cont)
        act_end = menu.addAction(lbl_end)

        if self.istart == 0:
            act_start.setEnabled(True)
            act_pause.setEnabled(False)
            act_cont.setEnabled(False)
        elif self.ipause == 0:
            act_start.setEnabled(False)
            act_pause.setEnabled(True)
            act_cont.setEnabled(False)
        else:
            act_start.setEnabled(True)
            act_pause.setEnabled(False)
            act_cont.setEnabled(self.icrho == 0 and self.icstate == 0)

        chosen = menu.exec_(event.globalPos())
        if chosen == act_start:
            self.handle_start()
        elif chosen == act_pause:
            self.handle_pause()
        elif chosen == act_cont:
            self.handle_continue()
        elif chosen == act_end:
            self.handle_end()

    def handle_start(self):
        if self.itset == 0 or self.irhoset == 0:
            QMessageBox.information(
                self, self.messages.get("titulo", "DiMol2D"),
                self.messages.get("hayqtd", "Temperature and density must be chosen before the start of the run")
            )
            return

        if self.icn == 1:
            self.icn = 0
            self.NMOL = self.NMOLN
            if self.itype == 1:
                self.natcm[1] = self.NMOL
            if self.itype > 1:
                self.set_nparta(self.xmola)
            if self.itype == 3:
                self.set_npartb(self.xmolb)

        self.inic = 0
        self.ib = 0
        self.rstate[self.istate] = self.densidad
        self.tstate[self.istate] = self.temperatura
        self.istate = (self.istate + 1) % len(self.rstate)

        self.rsize = math.sqrt(self.densidad / float(self.NMOL))
        self.SIZE = int(self.HSIZE * self.rsize + 0.501)
        self.rimin = (self.rmin * self.rsize) ** 2
        self.rimax = (self.rmax * self.rsize) ** 2
        self.ngrm = min(int(1.0 / (2.0 * self.rsize * self.drg) + 0.51), self.NGRMAX - 1)
        self.ngrmy = min(int(1.0 / (self.rsize * self.drg) + 0.51), self.NGRMAXY - 1)
        self.drgx = (1.0 / self.rsize) / float(self.ngrm)
        self.drgy = (1.0 / self.rsize) / float(self.ngrm)

        self.inicp = 1
        self.ipause = 0
        self.icrho = 0
        self.icstate = 0
        self.istart = 1
        self.main_win.sync_ui_running_state()
        self.timer.start()

    def handle_pause(self):
        if self.istart == 0:
            return
        self.timer.stop()
        self.ipause = 1
        self.idum_old = self.idum
        self.itype_old = self.itype
        self.natcm_old[:] = self.natcm[:]
        self.densidad_o = self.densidad
        self.main_win.sync_ui_running_state()
        self.update()

    def handle_continue(self):
        if self.istart == 0:
            return
        self.densidad = self.densidad_o
        self.IRHOINIT = int(100.0 * self.densidad + 0.501)
        self.main_win.rho_slider.blockSignals(True)
        self.main_win.rho_slider.setValue(self.IRHOINIT)
        self.main_win.rho_slider.blockSignals(False)
        self.main_win.prhoT.set_rt(self.temperatura, self.densidad)

        if self.icn == 1:
            self.icn = 0
            if self.main_win.npp_win is not None:
                self.main_win.npp_win.slider.setValue(self.NMOL)

        self.inic = 0 if self.inicp == 0 else 1
        self.inicp = 1
        self.ipause = 0
        self.icrho = 0
        self.icstate = 0
        self.main_win.sync_ui_running_state()
        self.timer.start()

    def handle_end(self):
        was_running = self.timer.isActive()
        self.timer.stop()
        ans = QMessageBox.question(
            self,
            self.messages.get("saliendo", "Exiting application..."),
            self.messages.get("confterm", "Exit the application ?"),
            QMessageBox.Yes | QMessageBox.No
        )
        if ans == QMessageBox.Yes:
            QApplication.quit()
        elif was_running:
            self.timer.start()

    def _step_simulation(self):
        fgy = 0.0 if self.igrav == 0 else self.fgy0
        self.natcm[0] = 0
        self.pres = self.engine.jsimul(
            self.ix, self.iy, self.ax, self.ay,
            self.gr1, self.gr2,
            self.drg, self.tistep, self.rcaa, self.rcab,
            self.temperatura, self.densidad, self.scaflg, self.ifrsca,
            self.itype, self.NMOL + 1, self.ngrm, self.ngrmy,
            self.HSIZE, self.RHSIZE, self.inic, self.ncp,
            self.gw1, self.gw2, self.gw3, self.gwy,
            self.rw1, self.rw2, self.rw3, self.rwy, self.epsw,
            self.iwall, self.iwally, fgy,
            self.natcm, self.idum, self.elon, self.iheter
        )

        self.indexrun1 += 1
        if self.indexrun1 % 20 == 0:
            self.main_win.manoM.set_pressure(self.pres)

        half_sz = self.SIZE // 2
        for i in range(1, self.NMOL + 1):
            self.ix[i] -= half_sz
            self.iy[i] -= half_sz

        self.inic = 1
        self.ib += 1
        self.update()

        if self.indexrun1 % 10 == 0:
            self.main_win.refresh_child_windows()

    def set_elon(self, eln: float):
        self.elon = eln
        self.icstate = 1

    def set_nparta(self, xma: float):
        self.xmola = xma
        if self.itype == 3:
            if self.xmola + self.xmolb > 1.0:
                self.xmolb = 1.0 - self.xmola
                if self.main_win.comp_win is not None and self.main_win.comp_win.xmolb_slider is not None:
                    self.main_win.comp_win.xmolb_slider.blockSignals(True)
                    self.main_win.comp_win.xmolb_slider.setValue(int(self.xmolb * 100.0))
                    self.main_win.comp_win.xmolb_slider.blockSignals(False)
                self.natcm[3] = self.NMOL
            self.xmolc = 1.0 - self.xmola - self.xmolb

        self.natcm[1] = int(xma * self.NMOL)
        if self.natcm[1] < self.idum:
            self.natcm[1] = self.idum

        if self.itype == 3:
            if self.natcm[1] >= self.NMOL - 1:
                self.natcm[1] = self.NMOL - 2
            if self.idum > 1 and (self.natcm[1] % self.idum != 0):
                self.natcm[1] = self.idum * (self.natcm[1] // self.idum)
            self.natcm[2] = self.natcm[1] + int(self.xmolb * self.NMOL)
            if self.natcm[2] == self.natcm[1]:
                self.natcm[2] = self.natcm[1] + 1
            if self.natcm[2] >= self.NMOL:
                self.natcm[2] = self.NMOL - 1
            self.natcm[3] = self.NMOL

        if self.itype == 2:
            self.xmolb = 1.0 - self.xmola
            self.natcm[2] = self.NMOL
            if self.natcm[1] < self.idum:
                self.natcm[1] = self.idum
            if self.natcm[1] >= self.NMOL:
                self.natcm[1] = self.NMOL - self.idum
            if self.idum > 1 and (self.natcm[1] % self.idum != 0):
                self.natcm[1] = self.idum * (self.natcm[1] // self.idum)

        self.icstate = 1

    def set_npartb(self, xmb: float):
        self.xmolb = xmb
        if self.xmola + self.xmolb > 1.0:
            self.xmola = 1.0 - self.xmolb
            if self.main_win.comp_win is not None:
                self.main_win.comp_win.xmola_slider.blockSignals(True)
                self.main_win.comp_win.xmola_slider.setValue(int(self.xmola * 100.0))
                self.main_win.comp_win.xmola_slider.blockSignals(False)
            self.natcm[1] = int(self.xmola * self.NMOL)

        if self.natcm[1] < self.idum:
            self.natcm[1] = self.idum
        if self.natcm[1] >= self.NMOL - 1:
            self.natcm[1] = self.NMOL - 2
        if self.idum > 1 and (self.natcm[1] % self.idum != 0):
            self.natcm[1] = self.idum * (self.natcm[1] // self.idum)

        self.natcm[2] = self.natcm[1] + int(xmb * self.NMOL)
        if self.natcm[2] == self.natcm[1]:
            self.natcm[2] = self.natcm[1] + 1
        if self.natcm[2] >= self.NMOL:
            self.natcm[2] = self.NMOL - 1
        self.natcm[3] = self.NMOL
        self.xmolc = 1.0 - self.xmola - self.xmolb
        self.icstate = 1

    # -------------------------------------------------------------------------
    # Rendering (exact translation of Simulation.java drawing routines)
    # -------------------------------------------------------------------------
    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)

        if not self.img_bg.isNull():
            p.drawPixmap(0, 0, self.RHSIZE, self.RVSIZE, self.img_bg)
        else:
            p.fillRect(self.rect(), QColor(200, 220, 235))

        self.pintacaja(p, self.HSIZE, self.VSIZE, self.RHSIZE, self.RVSIZE, self.SIZE)

        if self.istart != 0:
            use_old = (
                (
                    (self.idum == 2 and self.idum_old == 1)
                    or (self.idum == 2 and self.idum_old == 2 and self.itype < self.itype_old)
                    or (self.idum == 2 and self.natcm[1] > self.natcm_old[1])
                )
                and self.ipause == 1
            )
            if use_old:
                self.dibuja(
                    p, self.ix, self.iy, self.ax, self.ay, self.NMOL,
                    self.HSIZE, self.VSIZE, self.RHSIZE, self.RVSIZE, self.SIZE,
                    self.itype_old, self.idum_old, self.elon, self.natcm_old
                )
            else:
                self.dibuja(
                    p, self.ix, self.iy, self.ax, self.ay, self.NMOL,
                    self.HSIZE, self.VSIZE, self.RHSIZE, self.RVSIZE, self.SIZE,
                    self.itype, self.idum, self.elon, self.natcm
                )

        if self.istart == 0 or self.ipause == 1:
            self.draw_running_text(
                p,
                self.messages.get("runtexto", "Press the Right mouse button to"),
                self.messages.get("runtexto2", "access the Control Popup Menu")
            )

        self.pintacontador(p, self.ib)

    def pintacontador(self, p: QPainter, ind: int):
        if not self.img_contador.isNull():
            p.drawPixmap(self.RHSIZE - 134, self.RVSIZE - 42, self.img_contador)
        digits = [0] * 6
        ikk = ind
        for i in range(5, -1, -1):
            digits[5 - i] = ikk % 10
            ikk //= 10
        for i in range(6):
            d_pix = self.img_digits[digits[i]]
            if not d_pix.isNull():
                p.drawPixmap(self.RHSIZE - 124 + i * 19, self.RVSIZE - 35, d_pix)

    def _draw_outlined_text(self, p: QPainter, x: int, y: int, text: str):
        """Draw high-contrast text with dark shadow so it is clearly legible over drops21.jpg."""
        p.setPen(QColor(10, 20, 35, 220))
        for dx, dy in ((-2, -1), (2, 2), (-1, 2), (2, -1)):
            p.drawText(x + dx, y + dy, text)
        p.setPen(QColor(255, 248, 140))
        p.drawText(x, y, text)

    def draw_running_text(self, p: QPainter, texto: str, texto2: str):
        ir = self.indexrun
        p.setFont(QFont("Helvetica", 18, QFont.Bold))
        self._draw_outlined_text(p, ir, ir, texto)
        self._draw_outlined_text(p, self.HSIZE - ir, self.VSIZE - ir + 30, texto2)
        self._draw_outlined_text(p, self.HSIZE - ir, self.VSIZE - ir, texto)
        self._draw_outlined_text(p, ir, ir + 30, texto2)
        if not self.img_dm2d.isNull():
            p.drawPixmap(ir, self.VSIZE - ir, self.img_dm2d)
            p.drawPixmap(self.HSIZE - ir, ir, self.img_dm2d)

    def pintacaja(self, p: QPainter, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int):
        xorg = (rhsize - hsize) // 2
        yorg = (rvsize - vsize) // 2
        p.setPen(QPen(Qt.blue, 1.4))
        if self.iwally != 1:
            p.drawLine(0, yorg, rhsize, yorg)
            p.drawLine(0, yorg + vsize, rhsize, yorg + vsize)
        if self.iwall != 1:
            p.drawLine(xorg, 0, xorg, rvsize)
            p.drawLine(xorg + hsize, 0, xorg + hsize, rvsize)

        w_pad = int(0.835 * size) // 2
        if self.iwall == 1:
            p.fillRect(0, 0, xorg - w_pad, rvsize, COLOR_W)
            p.fillRect(xorg + hsize + w_pad, 0, rhsize - (xorg + hsize + w_pad), rvsize, COLOR_W)
        if self.iwally == 1:
            p.fillRect(0, 0, rhsize + 50, yorg - w_pad, COLOR_W)
            p.fillRect(0, yorg + vsize + w_pad, rhsize + 50, rvsize - (yorg + vsize + w_pad), COLOR_W)

    def dibuja(
        self, p: QPainter, lx, ly, ax, ay, nmol: int,
        hsize: int, vsize: int, rhsize: int, rvsize: int, size: int,
        ityp: int, idumb: int, elong: float, natcm
    ):
        if self.ipmol == 1:
            if ityp == 1:
                if idumb == 1:
                    for i in range(1, nmol + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_A)
                else:
                    for i in range(1, nmol, 2):
                        self.drawcirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, elong)
                        self.pintacirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, COLOR_B, COLOR_C, self.iheter, elong)
            elif ityp == 2:
                n1 = int(natcm[1])
                if idumb == 1:
                    for i in range(1, n1 + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_A)
                    for i in range(n1 + 1, nmol + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_B)
                else:
                    for i in range(1, n1, 2):
                        self.drawcirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, elong)
                        self.pintacirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, COLOR_B, COLOR_C, self.iheter, elong)
                    for i in range(n1 + 1, nmol + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_B)
            elif ityp == 3:
                n1 = int(natcm[1])
                n2 = int(natcm[2])
                if idumb == 1:
                    for i in range(1, n1 + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_A)
                    for i in range(n1 + 1, n2 + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_B)
                    for i in range(n2 + 1, nmol + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_C)
                else:
                    for i in range(1, n1, 2):
                        self.drawcirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, elong)
                        self.pintacirclerectang(p, int(lx[i]), int(lx[i + 1]), int(ly[i]), int(ly[i + 1]), hsize, vsize, rhsize, rvsize, size, COLOR_A, COLOR_B, COLOR_C, self.iheter, elong)
                    for i in range(n1 + 1, n2 + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_B)
                    for i in range(n2 + 1, nmol + 1):
                        self.pintamolecula(p, int(lx[i]), int(ly[i]), hsize, vsize, rhsize, rvsize, size, COLOR_C)

        if self.ipbon == 1:
            p.setPen(QPen(Qt.black, 1.1))
            half = size // 2
            for i in range(1, nmol + 1):
                axi, ayi = ax[i], ay[i]
                for j in range(i + 1, nmol + 1):
                    dx = axi - ax[j]
                    dy = ayi - ay[j]
                    r2 = dx * dx + dy * dy
                    if self.rimin <= r2 <= self.rimax:
                        p.drawLine(int(lx[i]) + half, int(ly[i]) + half, int(lx[j]) + half, int(ly[j]) + half)

    def pintamolecula(self, p: QPainter, x: int, y: int, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int, colormol: QColor):
        p.setBrush(QBrush(colormol))
        p.setPen(QPen(Qt.black, 1.0))
        p.drawEllipse(x, y, size, size)
        self.pinta_offbounds(p, x, y, hsize, vsize, rhsize, rvsize, size, colormol)

    def pinta_offbounds(self, p: QPainter, x: int, y: int, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int, colormol: QColor):
        p.setBrush(QBrush(colormol))
        p.setPen(Qt.NoPen)
        if x - hsize > 0 and self.iwall != 1:
            p.drawEllipse(x - hsize, y, size, size)
            if y - vsize > 0 and self.iwally != 1:
                p.drawEllipse(x - hsize, y - vsize, size, size)
            if y + vsize < rvsize and self.iwally != 1:
                p.drawEllipse(x - hsize, y + vsize, size, size)
        if x + hsize < rhsize and self.iwall != 1:
            p.drawEllipse(x + hsize, y, size, size)
            if y - vsize > 0 and self.iwally != 1:
                p.drawEllipse(x + hsize, y - vsize, size, size)
            if y + vsize < rvsize and self.iwally != 1:
                p.drawEllipse(x + hsize, y + vsize, size, size)
        if y - vsize > 0 and self.iwally != 1:
            p.drawEllipse(x, y - vsize, size, size)
        if y + vsize < rvsize and self.iwally != 1:
            p.drawEllipse(x, y + vsize, size, size)

    def drawcirclerectang(self, p: QPainter, x1: int, x2: int, y1: int, y2: int, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int, colormol: QColor, elong: float):
        ex = float(x2 - x1)
        ey = float(y2 - y1)
        length = math.hypot(ex, ey)
        if length < 1e-9:
            return
        ex /= length
        ey /= length
        xs = int(ex * size / 2.0)
        ys = int(ey * size / 2.0)
        half = size // 2

        p.setPen(QPen(Qt.black, 1.0))
        p.setBrush(Qt.NoBrush)
        p.drawEllipse(x1, y1, size, size)
        p.drawEllipse(x2, y2, size, size)

        if abs(x1 - x2) < hsize // 2 and abs(y1 - y2) < vsize // 2:
            poly = QPolygon([
                QPoint(x1 + ys + half, y1 - xs + half),
                QPoint(x1 - ys + half, y1 + xs + half),
                QPoint(x2 - ys + half, y2 + xs + half),
                QPoint(x2 + ys + half, y2 - xs + half),
            ])
            p.drawPolygon(poly)

    def pintacirclerectang(self, p: QPainter, x1: int, x2: int, y1: int, y2: int, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int, colormol: QColor, colormol1: QColor, colormol2: QColor, iheter: int, elong: float):
        p.setPen(Qt.NoPen)
        if iheter == 1:
            self.pinta_offbounds(p, x1, y1, hsize, vsize, rhsize, rvsize, size, colormol1)
            p.setBrush(QBrush(colormol1))
            p.drawEllipse(x1, y1, size, size)
            p.setBrush(QBrush(colormol2))
            p.drawEllipse(x2, y2, size, size)
            self.pinta_offbounds(p, x2, y2, hsize, vsize, rhsize, rvsize, size, colormol2)
        else:
            self.pinta_offbounds(p, x1, y1, hsize, vsize, rhsize, rvsize, size, colormol)
            self.pinta_offbounds(p, x2, y2, hsize, vsize, rhsize, rvsize, size, colormol)
            p.setBrush(QBrush(colormol))
            p.drawEllipse(x1, y1, size, size)
            p.drawEllipse(x2, y2, size, size)

        self.fillrectangf(p, x1, x2, y1, y2, hsize, vsize, rhsize, rvsize, size, colormol, colormol1, colormol2, iheter)

    def fillrectangf(self, p: QPainter, x1: int, x2: int, y1: int, y2: int, hsize: int, vsize: int, rhsize: int, rvsize: int, size: int, colormol: QColor, colormol1: QColor, colormol2: QColor, iheter: int):
        if abs(x1 - x2) > hsize // 2:
            if x1 < hsize // 2:
                self.fillrectang(p, x1, x2 - hsize, y1, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
                self.fillrectang(p, x1 + hsize, x2, y1, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
            else:
                self.fillrectang(p, x1 - hsize, x2, y1, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
                self.fillrectang(p, x1, x2 + hsize, y1, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
        elif abs(y1 - y2) > vsize // 2:
            if y1 < vsize // 2:
                self.fillrectang(p, x1, x2, y1, y2 - vsize, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
                self.fillrectang(p, x1, x2, y1 + vsize, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
            else:
                self.fillrectang(p, x1, x2, y1 - vsize, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
                self.fillrectang(p, x1, x2, y1, y2 + vsize, hsize, vsize, size, colormol, colormol1, colormol2, iheter)
        else:
            self.fillrectang(p, x1, x2, y1, y2, hsize, vsize, size, colormol, colormol1, colormol2, iheter)

    def fillrectang(self, p: QPainter, x1: int, x2: int, y1: int, y2: int, hsize: int, vsize: int, size: int, colormol: QColor, colormol1: QColor, colormol2: QColor, iheter: int):
        if abs(x1 - x2) >= hsize // 2 or abs(y1 - y2) >= vsize // 2:
            return
        ex = float(x2 - x1)
        ey = float(y2 - y1)
        length = math.hypot(ex, ey)
        if length < 1e-9:
            return
        ex /= length
        ey /= length
        xs = int(ex * size / 2.0)
        ys = int(ey * size / 2.0)
        half = size // 2
        p.setPen(Qt.NoPen)

        if iheter == 0:
            p.setBrush(QBrush(colormol))
            poly = QPolygon([
                QPoint(x1 + ys + half, y1 - xs + half),
                QPoint(x1 - ys + half, y1 + xs + half),
                QPoint(x2 - ys + half, y2 + xs + half),
                QPoint(x2 + ys + half, y2 - xs + half),
            ])
            p.drawPolygon(poly)
        else:
            xm = (x1 + x2) // 2
            ym = (y1 + y2) // 2
            p.setBrush(QBrush(colormol1))
            poly1 = QPolygon([
                QPoint(x1 + ys + half, y1 - xs + half),
                QPoint(x1 - ys + half, y1 + xs + half),
                QPoint(xm - ys + half, ym + xs + half),
                QPoint(xm + ys + half, ym - xs + half),
            ])
            p.drawPolygon(poly1)

            p.setBrush(QBrush(colormol2))
            poly2 = QPolygon([
                QPoint(xm + ys + half, ym - xs + half),
                QPoint(xm - ys + half, ym + xs + half),
                QPoint(x2 - ys + half, y2 + xs + half),
                QPoint(x2 + ys + half, y2 - xs + half),
            ])
            p.drawPolygon(poly2)


# =============================================================================
# Main Application Window (DiMol2D.java)
# =============================================================================
class DiMol2DMainWindow(QMainWindow):
    """Main window assembling menus, simulation canvas, sliders, and gauges."""

    def __init__(self, messages: dict):
        super().__init__()
        self.messages = messages
        self.setWindowTitle(messages.get("titulo", "DiMol2D v1.1"))

        screen = QApplication.primaryScreen().availableGeometry()
        rhsize = min(820, (82 * screen.height()) // 100)
        rvsize = rhsize

        # Child window references
        self.gr_win = None
        self.pdx_win = None
        self.pdy_win = None
        self.ppd_win = None
        self.inter_win = None
        self.interw_win = None
        self.comp_win = None
        self.elon_win = None
        self.npp_win = None
        self.veloc_win = None
        self.help_win = None
        self.about_win = None

        self.mdSim = SimulationWidget(self, messages, rhsize, rvsize)
        self.manoM = ManometerWidget(messages, 0.0, 16, int(rvsize / 3.2))
        self.prhoT = PintaRTWidget(messages, 0.0, 0.0, 148, 56)

        self._build_menus()
        self._build_layout(rhsize, rvsize)

    def _build_menus(self):
        mbar = self.menuBar()
        mbar.setNativeMenuBar(False)

        # 1. Simulation Type menu
        self.menu_tipo = mbar.addMenu(self.messages.get("tipo", "Simulation type"))
        type_group = QActionGroup(self)
        type_group.setExclusive(True)

        self.act_pure = QAction(self.messages.get("sustanciapura", "Pure Substance"), self, checkable=True)
        self.act_pure.setChecked(True)
        self.act_pure.triggered.connect(lambda: self._set_sim_type(1))
        type_group.addAction(self.act_pure)
        self.menu_tipo.addAction(self.act_pure)

        self.act_bin = QAction(self.messages.get("mezclabinaria", "Binary Mixture"), self, checkable=True)
        self.act_bin.triggered.connect(lambda: self._set_sim_type(2))
        type_group.addAction(self.act_bin)
        self.menu_tipo.addAction(self.act_bin)

        self.act_ter = QAction(self.messages.get("mezclaternaria", "Ternary Mixture"), self, checkable=True)
        self.act_ter.triggered.connect(lambda: self._set_sim_type(3))
        type_group.addAction(self.act_ter)
        self.menu_tipo.addAction(self.act_ter)

        self.act_dumb = QAction(self.messages.get("dumbell", "Diatomics (Component A)"), self, checkable=True)
        self.act_dumb.triggered.connect(self._toggle_dumbbell)
        self.menu_tipo.addAction(self.act_dumb)

        self.act_het = QAction(self.messages.get("het", "Heteronuclear (A)"), self, checkable=True)
        self.act_het.triggered.connect(self._toggle_hetero)
        self.menu_tipo.addAction(self.act_het)

        self.menu_tipo.addSeparator()

        self.act_wall = QAction(self.messages.get("paredeslaterales", "Lateral Walls"), self, checkable=True)
        self.act_wall.triggered.connect(self._toggle_wallx)
        self.menu_tipo.addAction(self.act_wall)

        self.act_wally = QAction(self.messages.get("paredeshorizontales", "Horizontal Walls"), self, checkable=True)
        self.act_wally.triggered.connect(self._toggle_wally)
        self.menu_tipo.addAction(self.act_wally)

        self.act_grav = QAction(self.messages.get("gravitacion", "Gravity"), self, checkable=True)
        self.act_grav.triggered.connect(self._toggle_gravity)
        self.menu_tipo.addAction(self.act_grav)

        # 2. System Control & Properties menu
        self.menu_sis = mbar.addMenu(self.messages.get("sistema", "System Control & Properties"))
        self.menu_inter = self.menu_sis.addMenu(self.messages.get("interacciones", "Interactions"))

        self.act_interat = self.menu_inter.addAction(self.messages.get("atomoatomo", "Atom-Atom"))
        self.act_interat.triggered.connect(self._open_inter_atom)

        self.act_interw = self.menu_inter.addAction(self.messages.get("atomopared", "Wall-Atom"))
        self.act_interw.setEnabled(False)
        self.act_interw.triggered.connect(self._open_inter_wall)

        self.act_compos = self.menu_sis.addAction(self.messages.get("composition", "Composition"))
        self.act_compos.setEnabled(False)
        self.act_compos.triggered.connect(self._open_compos)

        self.act_elong = self.menu_sis.addAction(self.messages.get("elong", "Elongation"))
        self.act_elong.setEnabled(False)
        self.act_elong.triggered.connect(self._open_elong)

        self.act_disten = self.menu_sis.addAction(self.messages.get("distanciaenlace", "Bond distance"))
        self.act_disten.triggered.connect(self._ask_bond_distance)

        self.act_natoms = self.menu_sis.addAction(self.messages.get("numeroparticulas", "Particle number"))
        self.act_natoms.triggered.connect(self._open_natoms)

        self.act_veloc = self.menu_sis.addAction(self.messages.get("sincro", "Visualization synchrony"))
        self.act_veloc.triggered.connect(self._open_veloc)

        self.act_print = self.menu_sis.addAction(self.messages.get("imprimir", "Print"))
        self.act_print.triggered.connect(self._print_snapshot)

        self.menu_sis.addSeparator()

        self.act_pgr = self.menu_sis.addAction(self.messages.get("gr", "Pair distribution function"))
        self.act_pgr.triggered.connect(self._open_gr)

        self.act_pdpx = self.menu_sis.addAction(self.messages.get("perfdenshor", "Horizontal density profile"))
        self.act_pdpx.setEnabled(False)
        self.act_pdpx.triggered.connect(self._open_pdx)

        self.act_pdpy = self.menu_sis.addAction(self.messages.get("perfdensver", "Vertical density profile"))
        self.act_pdpy.setEnabled(False)
        self.act_pdpy.triggered.connect(self._open_pdy)

        self.act_phd = self.menu_sis.addAction(self.messages.get("diagramafases", "Phase diagram"))
        self.act_phd.triggered.connect(self._open_phd)

        self.menu_sis.addSeparator()

        self.act_rbMol = QAction(self.messages.get("pintamoleculas", "Paint Molecules"), self, checkable=True)
        self.act_rbMol.setChecked(True)
        self.act_rbMol.triggered.connect(lambda checked: setattr(self.mdSim, "ipmol", 1 if checked else 0))
        self.menu_sis.addAction(self.act_rbMol)

        self.act_rbBon = QAction(self.messages.get("pintaenlaces", "Paint Bonds"), self, checkable=True)
        self.act_rbBon.triggered.connect(lambda checked: setattr(self.mdSim, "ipbon", 1 if checked else 0))
        self.menu_sis.addAction(self.act_rbBon)

        # 3. Help menu
        self.menu_help = mbar.addMenu(self.messages.get("ayuda", "Help"))
        self.act_help = self.menu_help.addAction(self.messages.get("ayudabasica", "Basic Help"))
        self.act_help.triggered.connect(self._open_help)
        self.menu_help.addSeparator()
        self.act_about = self.menu_help.addAction(self.messages.get("acerca", "About DiMol2D"))
        self.act_about.triggered.connect(self._open_about)

    def _build_layout(self, rhsize: int, rvsize: int):
        central = QWidget()
        self.setCentralWidget(central)

        main_h = QHBoxLayout(central)
        main_h.setContentsMargins(4, 4, 6, 4)
        main_h.setSpacing(8)
        main_h.addWidget(self.mdSim)

        right_v = QVBoxLayout()
        right_v.setSpacing(6)

        # QFT Icon
        lbl_qft = QLabel()
        pix_qft = QPixmap(resource_path("Icon/qft_t100.png"))
        if not pix_qft.isNull():
            lbl_qft.setPixmap(pix_qft)
        lbl_qft.setAlignment(Qt.AlignCenter)
        right_v.addWidget(lbl_qft)

        # Sliders section with numeric scales
        sliders_grid = QGridLayout()
        sliders_grid.setHorizontalSpacing(4)
        lbl_t = QLabel()
        pix_t = QPixmap(resource_path("Icon/T.png"))
        if not pix_t.isNull():
            lbl_t.setPixmap(pix_t)
        lbl_t.setAlignment(Qt.AlignCenter)

        lbl_rho = QLabel()
        pix_rho = QPixmap(resource_path("Icon/Rho.png"))
        if not pix_rho.isNull():
            lbl_rho.setPixmap(pix_rho)
        lbl_rho.setAlignment(Qt.AlignCenter)

        sliders_grid.addWidget(lbl_t, 0, 0, Qt.AlignCenter)
        sliders_grid.addWidget(lbl_rho, 0, 1, Qt.AlignCenter)

        tr_size = int(rvsize / 3.2)
        self.temp_slider = LabeledSlider(
            Qt.Vertical, minimum=0, maximum=200, value=0,
            major_step=50, minor_step=25, scale_factor=0.01, decimals=1
        )
        self.temp_slider.setFixedHeight(tr_size)
        self.temp_slider.valueChanged.connect(self._on_temp_changed)
        self.temp_slider.sliderReleased.connect(self._on_temp_released)

        self.rho_slider = LabeledSlider(
            Qt.Vertical, minimum=0, maximum=100, value=0,
            major_step=20, minor_step=10, scale_factor=0.01, decimals=1
        )
        self.rho_slider.setFixedHeight(tr_size)
        self.rho_slider.valueChanged.connect(self._on_rho_changed)
        self.rho_slider.sliderReleased.connect(self._on_rho_released)

        sliders_grid.addWidget(self.temp_slider, 1, 0, Qt.AlignCenter)
        sliders_grid.addWidget(self.rho_slider, 1, 1, Qt.AlignCenter)
        right_v.addLayout(sliders_grid)

        right_v.addWidget(self.prhoT, 0, Qt.AlignCenter)
        right_v.addWidget(self.manoM, 0, Qt.AlignCenter)
        right_v.addStretch(1)

        main_h.addLayout(right_v)
        self.setFixedSize(rhsize + 178, rvsize + 38)

    # -------------------------------------------------------------------------
    # Slider & Menu Callbacks
    # -------------------------------------------------------------------------
    def _on_temp_changed(self, val):
        ttemp = val / 100.0
        self.mdSim.temperatura = ttemp
        self.mdSim.itset = 1
        self.prhoT.set_rt(self.mdSim.temperatura, self.mdSim.densidad)
        if self.ppd_win is not None:
            self.ppd_win.ptransient(ttemp, self.mdSim.densidad)

    def _on_temp_released(self):
        val = self.temp_slider.value()
        self.mdSim.temperatura = val / 100.0
        self.mdSim.tstate[self.mdSim.istate] = self.mdSim.temperatura
        self.mdSim.rstate[self.mdSim.istate] = self.mdSim.densidad
        self.mdSim.istate = (self.mdSim.istate + 1) % len(self.mdSim.tstate)
        self.mdSim.itset = 1
        self.prhoT.set_rt(self.mdSim.temperatura, self.mdSim.densidad)
        if self.ppd_win is not None:
            self.ppd_win.ptransient(self.mdSim.temperatura, self.mdSim.densidad)

    def _on_rho_changed(self, val):
        if self.mdSim.istart != 0 and self.mdSim.ipause != 1:
            return
        irho = max(1, val)
        self.mdSim.irho = irho
        self.mdSim.densidad = irho / 100.0
        self.mdSim.irhoset = 1
        self.mdSim.icrho = 1
        self.prhoT.set_rt(self.mdSim.temperatura, self.mdSim.densidad)
        if self.ppd_win is not None:
            self.ppd_win.ptransient(self.mdSim.temperatura, self.mdSim.densidad)

    def _on_rho_released(self):
        if self.mdSim.istart != 0 and self.mdSim.ipause != 1:
            return
        self._on_rho_changed(self.rho_slider.value())

    def _set_sim_type(self, itype: int):
        if self.mdSim.istart != 0 and self.mdSim.ipause != 1:
            return
        if self.mdSim.itype != itype:
            self.mdSim.inicp = 0
            self.mdSim.icstate = 1
        self.mdSim.itype = itype

        if itype == 1:
            self.mdSim.natcm[1] = self.mdSim.NMOL
            self.act_compos.setEnabled(False)
            self.act_elong.setEnabled(self.mdSim.idum > 1)
            if self.comp_win is not None:
                self.comp_win.close()
        elif itype == 2:
            self.mdSim.natcm[1] = self.mdSim.NMOL // 2
            if self.mdSim.idum > 1 and (self.mdSim.natcm[1] % self.mdSim.idum != 0):
                self.mdSim.natcm[1] = self.mdSim.idum * (self.mdSim.natcm[1] // self.mdSim.idum)
            self.mdSim.natcm[2] = self.mdSim.NMOL
            self.mdSim.xmola = 0.5
            self.mdSim.xmolb = 0.5
            self.act_compos.setEnabled(True)
            self.act_elong.setEnabled(self.mdSim.idum > 1)
        elif itype == 3:
            self.mdSim.natcm[1] = self.mdSim.NMOL // 3
            if self.mdSim.idum > 1 and (self.mdSim.natcm[1] % self.mdSim.idum != 0):
                self.mdSim.natcm[1] = self.mdSim.idum * (self.mdSim.natcm[1] // self.mdSim.idum)
            self.mdSim.natcm[2] = (2 * self.mdSim.NMOL) // 3
            self.mdSim.natcm[3] = self.mdSim.NMOL
            self.mdSim.xmola = 1.0 / 3.0
            self.mdSim.xmolb = 1.0 / 3.0
            self.mdSim.xmolc = 1.0 / 3.0
            self.act_compos.setEnabled(True)
            self.act_elong.setEnabled(self.mdSim.idum > 1)

        if self.inter_win is not None:
            self.inter_win.close()
            self._open_inter_atom()
        if self.interw_win is not None:
            self.interw_win.close()
            self._open_inter_wall()
        if self.comp_win is not None and itype > 1:
            self.comp_win.close()
            self._open_compos()
        if self.ppd_win is not None:
            self.ppd_win.update()

    def _toggle_dumbbell(self, checked: bool):
        if checked:
            self.mdSim.idum = 2
            self.act_elong.setEnabled(True)
            if self.mdSim.itype in (2, 3):
                n1 = self.mdSim.NMOL // self.mdSim.itype
                if n1 % 2 != 0:
                    n1 = 2 * (n1 // 2)
                self.mdSim.natcm[1] = n1
        else:
            if self.mdSim.iheter == 1:
                self.mdSim.idum = 2
                self.act_dumb.setChecked(True)
            else:
                self.mdSim.idum = 1
                self.act_elong.setEnabled(False)
        self.mdSim.icstate = 1
        self.mdSim.inicp = 0

    def _toggle_hetero(self, checked: bool):
        if checked:
            self.mdSim.idum = 2
            self.mdSim.iheter = 1
            self.act_elong.setEnabled(True)
            self.act_dumb.setChecked(True)
            if self.mdSim.itype in (2, 3):
                n1 = self.mdSim.NMOL // self.mdSim.itype
                if n1 % 2 != 0:
                    n1 = 2 * (n1 // 2)
                self.mdSim.natcm[1] = n1
        else:
            self.mdSim.iheter = 0
        self.mdSim.icstate = 1
        self.mdSim.inicp = 0

    def _toggle_wallx(self, checked: bool):
        self.mdSim.iwall = 1 if checked else 0
        self.act_interw.setEnabled(checked)
        self.act_pdpx.setEnabled(checked)
        self.mdSim.icstate = 1
        self.mdSim.inicp = 0
        self.mdSim.update()

    def _toggle_wally(self, checked: bool):
        if checked:
            self.mdSim.iwally = 1
            self.act_pdpy.setEnabled(True)
        else:
            self.mdSim.iwally = 0
            self.mdSim.igrav = 0
            self.act_pdpy.setEnabled(False)
            self.act_grav.setChecked(False)
        self.mdSim.icstate = 1
        self.mdSim.inicp = 0
        self.mdSim.update()

    def _toggle_gravity(self, checked: bool):
        if checked:
            if not self.act_wally.isChecked():
                self.act_wally.setChecked(True)
                self.mdSim.iwally = 1
                self.act_pdpy.setEnabled(True)
            self.mdSim.igrav = 1
        else:
            self.mdSim.igrav = 0
        self.mdSim.icstate = 1
        self.mdSim.inicp = 0
        self.mdSim.update()

    def _ask_bond_distance(self):
        was_running = self.mdSim.timer.isActive()
        self.mdSim.timer.stop()
        rmax, ok1 = QInputDialog.getDouble(
            self, self.messages.get("distanciaenlace", "Bond distance"),
            self.messages.get("distmax", "Maximum bond distance"),
            self.mdSim.rmax, 0.1, 10.0, 2
        )
        rmin, ok2 = QInputDialog.getDouble(
            self, self.messages.get("distanciaenlace", "Bond distance"),
            self.messages.get("distmin", "Minimum bond distance"),
            self.mdSim.rmin, 0.0, 10.0, 2
        )
        if ok2:
            self.mdSim.rmin = max(0.0, rmin)
        if ok1:
            self.mdSim.rmax = rmax if rmax > self.mdSim.rmin else self.mdSim.rmin + 0.1
        self.mdSim.rimin = (self.mdSim.rmin * self.mdSim.rsize) ** 2
        self.mdSim.rimax = (self.mdSim.rmax * self.mdSim.rsize) ** 2
        if was_running:
            self.mdSim.timer.start()

    def _open_inter_atom(self):
        if self.inter_win is None:
            self.inter_win = InteraccionWindow(self.mdSim, lambda: setattr(self, "inter_win", None))
            self.inter_win.show()

    def _open_inter_wall(self):
        if self.interw_win is None:
            self.interw_win = InteraccionWallWindow(self.mdSim, lambda: setattr(self, "interw_win", None))
            self.interw_win.show()

    def _open_compos(self):
        if self.comp_win is None:
            self.comp_win = ComposicionWindow(self.mdSim, lambda: setattr(self, "comp_win", None))
            self.comp_win.show()

    def _open_elong(self):
        if self.elon_win is None:
            self.elon_win = ElongacionWindow(self.mdSim, lambda: setattr(self, "elon_win", None))
            self.elon_win.show()

    def _open_natoms(self):
        if self.npp_win is None:
            self.npp_win = SetNpartWindow(self.mdSim, lambda: setattr(self, "npp_win", None))
            self.npp_win.show()

    def _open_veloc(self):
        if self.veloc_win is None:
            self.veloc_win = SetVelocWindow(self.mdSim, lambda: setattr(self, "veloc_win", None))
            self.veloc_win.show()

    def _open_gr(self):
        if self.gr_win is None and self.mdSim.istart != 0:
            self.gr_win = GrWindow(self.mdSim, lambda: setattr(self, "gr_win", None))
            self.gr_win.show()

    def _open_pdx(self):
        if self.pdx_win is None and self.mdSim.istart != 0:
            self.pdx_win = PdxWindow(self.mdSim, lambda: setattr(self, "pdx_win", None))
            self.pdx_win.show()

    def _open_pdy(self):
        if self.pdy_win is None and self.mdSim.istart != 0:
            self.pdy_win = PdyWindow(self.mdSim, lambda: setattr(self, "pdy_win", None))
            self.pdy_win.show()

    def _open_phd(self):
        if self.ppd_win is None and self.mdSim.istate > 0:
            self.ppd_win = PhaseDiagramWindow(self.mdSim, lambda: setattr(self, "ppd_win", None))
            self.ppd_win.show()

    def _open_help(self):
        if self.help_win is None:
            self.help_win = HelpWindow(self.mdSim, lambda: setattr(self, "help_win", None))
            self.help_win.show()

    def _open_about(self):
        if self.about_win is None:
            self.about_win = AboutWindow(self.mdSim, lambda: setattr(self, "about_win", None))
            self.about_win.show()

    def _print_snapshot(self):
        printer = QPrinter(QPrinter.HighResolution)
        printer.setOutputFileName(f"SnapShot{self.mdSim.ib}.pdf")
        dlg = QPrintDialog(printer, self)
        if dlg.exec_() == QDialog.Accepted:
            p = QPainter(printer)
            rect = printer.pageRect()
            side = min(rect.width(), int(rect.height() * 0.8))
            pix = self.mdSim.grab()
            p.drawPixmap(0, 0, side, side, pix)
            p.setFont(QFont("Helvetica", 12, QFont.StyleItalic))
            p.drawText(0, side + 40, f"{self.messages.get('pasodesimulacion', 'Simulation step')} = {self.mdSim.ib}")
            p.drawText(0, side + 80, f"{self.messages.get('densidad', 'Density')} = {self.mdSim.densidad:.2f}")
            p.drawText(0, side + 120, f"{self.messages.get('temperatura', 'Temperature')} = {self.mdSim.temperatura:.2f}")
            p.end()

    def sync_ui_running_state(self):
        running = (self.mdSim.istart != 0 and self.mdSim.ipause == 0)
        self.act_natoms.setEnabled(not running)
        self.act_print.setEnabled(not running)
        self.act_wall.setEnabled(not running)
        self.act_wally.setEnabled(not running)
        self.act_grav.setEnabled(not running)
        self.act_dumb.setEnabled(not running)
        self.act_het.setEnabled(not running)
        self.act_elong.setEnabled((not running) and self.mdSim.idum == 2)
        self.menu_inter.setEnabled(not running)
        self.act_compos.setEnabled((not running) and self.mdSim.itype > 1)
        self.act_pure.setEnabled(not running)
        self.act_bin.setEnabled(not running)
        self.act_ter.setEnabled(not running)
        self.rho_slider.setEnabled(not running)

        for w in (self.inter_win, self.interw_win, self.comp_win, self.elon_win, self.npp_win):
            if w is not None:
                w.update_enabled()

    def refresh_child_windows(self):
        for w in (self.gr_win, self.pdx_win, self.pdy_win, self.ppd_win):
            if w is not None:
                w.update()


def main():
    parser = argparse.ArgumentParser(description="DiMol2D v1.1 - 2D Molecular Dynamics Visualization")
    parser.add_argument("--lang", choices=["en", "es"], default=None, help="Interface language (en or es)")
    args, qt_args = parser.parse_known_args()

    app = QApplication([sys.argv[0]] + qt_args)
    app.setStyleSheet(GLOBAL_STYLESHEET)
    messages = load_messages(args.lang)
    win = DiMol2DMainWindow(messages)
    win.show()
    sys.exit(app.exec_())


if __name__ == "__main__":
    main()
