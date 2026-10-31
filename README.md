# DiMol2D v1.1 — 2D Molecular Dynamics Visualization Suite (Python + Numba + PyQt5)

![DiMol2D Banner](Image/DM2D1-1s.png)

**DiMol2D** is an interactive educational and scientific Molecular Dynamics (MD) simulation and visualization program for two-dimensional fluids. Originally developed in Java Swing + C (JNI) + Fortran 90 by **E. Lomba**, **N. G. Almarza**, and **C. Martín** at the *Instituto de Química Física Rocasolano (IQFR-CSIC)*, Madrid, this repository provides a complete, cross-platform **Python + Numba + PyQt5** port.

---

## Features & Physical Models

All simulations are performed in two-dimensional Lennard-Jones (LJ) reduced units ($\sigma = 1$, $\varepsilon = 1$, $m_A = 1$, $k_B = 1$):

- **Thermodynamic Systems**:
  - **Pure Substance** (Component A)
  - **Binary Mixture** (Components A + B, with adjustable molar composition $x_A$)
  - **Ternary Mixture** (Components A + B + C, with adjustable molar fractions $x_A, x_B, x_C$)
- **Molecular Geometry & Holonomic Constraints**:
  - Monomeric disks or **diatomic dumbbells** (Component A) with adjustable bond elongation $d/\sigma \in [0.1, 1.0]$.
  - **Homonuclear** or **heteronuclear** diatomic dumbbells integrated via the 2-stage Velocity-Verlet + **RATTLE** constraint algorithm.
- **Pair & External Potentials**:
  - **Atom–Atom**: Truncated and shifted 12–6 Lennard-Jones potential with independent cutoff radii $r_c^{\alpha\alpha}$ (like species) and $r_c^{\alpha\beta}$ (unlike species), allowing continuous tuning from purely repulsive WCA ($r_c \approx 1.12\,\sigma$) to full attractive LJ ($r_c = 2.5\,\sigma$).
  - **Confining Walls**: Optional lateral ($x$) and/or horizontal ($y$) planar walls modeled with a 10–4 Mie/Steele wall potential $w(z)$ with tunable well depth $\varepsilon_w$ and species-specific cutoffs $r_{cw}$, enabling wetting, drying, and capillary condensation studies.
  - **Gravitational Field**: Optional vertical gravity ($g$) combined with horizontal walls to observe sedimentation and barometric profiles.
- **Real-Time Diagnostics & Plots**:
  - Radial distribution functions $g_{AA}(r)$ and $g_{AB}(r)$ (`g(r)` window).
  - Horizontal ($\rho_\alpha(x)$) and vertical ($\rho(y)$) density profiles near confining walls.
  - Interactive $(\rho^*, T^*)$ **Phase Diagram** with precalculated Gas, Liquid, Solid, Coexistence ($L+V$, $S+V$), and liquid-liquid demixing boundaries.
  - Live digital $(T^*, \rho^*)$ display, 6-digit MD step counter, and color-gradient pressure manometer.
  - Bilingual interface & embedded HTML user guide (**Spanish** and **English**).

---

## Project Structure

| File / Directory | Description |
| :--- | :--- |
| `dimol2d.py` | Main PyQt5 GUI application: main window, simulation canvas, custom `LabeledSlider` widgets with tick scales, interactive parameter dialogs, and live scientific plots. |
| `md_engine.py` | Numba-accelerated (`@njit(fastmath=True, cache=True)`) 2D Molecular Dynamics engine translating `MD-mezcla-par.f90` (linked-cell $O(N)$ forces, Velocity-Verlet/RATTLE integrator, Berendsen thermostat, RDF and wall profile accumulators). |
| `phase_data.py` | Precalculated 2D coexistence curves and polygon regions for the phase diagram viewer. |
| `MessagesBundle*.properties` | English (`MessagesBundle.properties`) and Spanish (`MessagesBundle_es.properties`) localization bundles. |
| `Icon/`, `Image/` | UI icons, splash images, and digital counter sprites. |
| `HelpFiles/`, `FicherosAyuda/` | Embedded HTML documentation in English and Spanish. |
| `legacy_java/` | Original Java Swing sources (`*.java`), C JNI wrapper (`Interface.c`, `Simulation.h`), Fortran 90 engine (`MD-mezcla-par.f90`), and platform Makefiles. |

---

## Installation & Usage

### 1. Install Dependencies

Requires Python 3.9+ with `numpy`, `numba`, and `PyQt5`:

```bash
pip install -r requirements.txt
```

### 2. Run DiMol2D

```bash
# Auto-detects system locale (Spanish or English)
python3 dimol2d.py

# Explicitly select Spanish or English UI
python3 dimol2d.py --lang es
python3 dimol2d.py --lang en
```

### 3. Quick Start Controls
1. Set an initial **Temperature ($T^*$)** and **Density ($\rho^*$)** using the vertical sliders on the right panel.
2. **Left-click** inside the simulation canvas to open the context menu (`Start`, `Pause`, `Restart averages`, `Increase/Decrease velocidad`, `Zero velocities`, `Stop`).
3. Use the **Simulation Type** and **System Control & Properties** menus to switch between pure/binary/ternary fluids, enable diatomic dumbbells or confining walls, and open real-time plots (`g(r)`, density profiles, phase diagram).

---

## Authors & Credits

- **Original Design & Physics Engine**: E. Lomba, N. G. Almarza, and C. Martín (*Instituto de Química Física Rocasolano, CSIC*, Madrid, Spain).
- **Python + Numba + PyQt5 Port**: E. Lomba.
