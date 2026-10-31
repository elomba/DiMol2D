"""
2D Molecular Dynamics Engine for DiMol2D (Python + Numba translation of MD-mezcla-par.f90).

Physical Model & Reduced Units
------------------------------
All quantities are expressed in Lennard-Jones (LJ) reduced units (length in units of
atomic diameter ``sigma``, energy in units of well depth ``epsilon``, mass in units of
Component A atomic mass ``m_A``, and temperature in units of ``epsilon / k_B``):

- **Pair potential (atom-atom)**: Truncated and shifted 12-6 Lennard-Jones potential
  $$u_{ij}(r) = 4 [(\\sigma/r)^{12} - (\\sigma/r)^6] - u_{\\text{shift}}(r_c)$$
  with separate cutoffs ``rci`` ($r_c^{\\alpha\\alpha}$ for like species) and ``rcd``
  ($r_c^{\\alpha\\beta}$ for unlike species). Setting $r_c = 2^{1/6}\\,\\sigma \\approx 1.1225$
  yields a purely repulsive Weeks-Chandler-Andersen (WCA) interaction.
- **Wall-particle potential**: Truncated and shifted 10-4 Mie/Steele planar wall potential
  $$w(z) = \\frac{5}{3}\\left(\\frac{5}{2}\\right)^{2/3} \\varepsilon_w
  \\left[\\left(\\frac{\\sigma}{z}\\right)^{10} - \\left(\\frac{\\sigma}{z}\\right)^4\\right] - w(z_c)$$
  applied at lateral ($x$) and/or horizontal ($y$) boundaries, plus an optional vertical
  gravitational field $U_{\\text{grav}}(y) = m_i g y$.
- **Molecular constraints**: Component A can form homonuclear or heteronuclear diatomic
  dumbbells with fixed bond length ``elong``, integrated via the 2-stage Velocity-Verlet
  + RATTLE holonomic constraint algorithm (`MOVES2`).
- **Force evaluation**: Linked-cell list (`Liscv` + half-shell neighbor stencil) providing
  $O(N)$ force evaluation in reduced box coordinates $[0, 1)^2$.
"""

import math
import numpy as np
from numba import njit

MATS = 4000
NLMX = 50
NCMX = NLMX * NLMX
NGMX = 5000
PI = 3.1415926535898


@njit(cache=True)
def build_neighbor_cells(nl, lparedx, lparedy):
    """Build half-shell linked-cell neighbor lists (Liscv in Fortran)."""
    nl = min(NLMX, max(nl, 3))
    nl2 = nl * nl
    lcv = np.zeros((4, nl2), dtype=np.int32)
    ncv = np.zeros(nl2, dtype=np.int32)

    for i in range(nl2):
        ix = i // nl
        iy = i % nl
        lmm = True
        l0m = True
        lpm = True
        lm0 = True
        if lparedx:
            lmm = ix > 0
            lm0 = ix > 0
            lpm = ix < nl - 1
        if lparedy:
            lmm = lmm and (iy > 0)
            l0m = iy > 0
            lpm = lpm and (iy > 0)

        ixm = (nl + ix - 1) % nl
        ixp = (ix + 1) % nl
        iym = (nl + iy - 1) % nl

        ncvi = 0
        if lmm:
            lcv[ncvi, i] = ixm * nl + iym
            ncvi += 1
        if l0m:
            lcv[ncvi, i] = ix * nl + iym
            ncvi += 1
        if lpm:
            lcv[ncvi, i] = ixp * nl + iym
            ncvi += 1
        if lm0:
            lcv[ncvi, i] = ixm * nl + iy
            ncvi += 1
        ncv[i] = ncvi

    return nl, nl2, lcv, ncv


@njit(cache=True)
def init_lattice(nats, idum, ntcm1, elon):
    """Initialize 2D lattice positions (REDES in Fortran)."""
    x = np.zeros(nats, dtype=np.float64)
    y = np.zeros(nats, dtype=np.float64)
    x1 = np.zeros(nats, dtype=np.float64)
    y1 = np.zeros(nats, dtype=np.float64)

    nc = (1 + nats) // 2
    mxo = 1
    myo = 1
    dvmx = 0.0
    mx = 0
    my = 0

    while my <= mx:
        my += 1
        mx = 1 + int(float(nc) / float(my) - 0.0001)
        if mx >= my:
            dx = 1.0 / float(mx)
            dy = 1.0 / float(my)
            d11 = 0.5 * math.sqrt(dx * dx + dy * dy)
            dv = min(dx, d11)
            if dv > dvmx:
                dvmx = dv
                mxo = mx
                myo = my

    mx = mxo
    my = myo
    dx = 1.0 / float(mx)
    dy = 1.0 / float(my)

    for i in range(nats):
        ic = i // 2
        ix = ic % mx
        iy = ic // mx
        ipc = i % 2
        x1[i] = ix * dx + ipc * (0.5 * dx)
        y1[i] = iy * dy + ipc * (0.5 * dy)

    if idum == 1:
        perm = np.random.permutation(nats)
        for i in range(nats):
            x[perm[i]] = x1[i]
            y[perm[i]] = y1[i]
    else:
        nobusy = np.ones(nats + 1, dtype=np.bool_)
        i = 0
        while i < ntcm1:
            count = int(np.random.random() * nats)
            # Fortran 1-based odd count corresponds to 0-based even index
            if (count % 2 == 0) and (count + 1 < nats) and nobusy[count] and nobusy[count + 1]:
                x[i] = x1[count]
                x[i + 1] = x1[count] + 0.5 * elon
                y[i] = y1[count]
                y[i + 1] = y1[count] + 0.5 * math.sqrt(3.0) * elon
                nobusy[count] = False
                nobusy[count + 1] = False
                i += 2
        i = ntcm1
        while i < nats:
            count = int(np.random.random() * nats)
            if nobusy[count]:
                x[i] = x1[count]
                y[i] = y1[count]
                nobusy[count] = False
                i += 1

    return x, y


@njit(cache=True)
def init_velocities(nats, temp, idum, ntcm1, elon, x, y, amass):
    """Initialize velocities and remove center-of-mass drift (SETV0 in Fortran)."""
    vx = np.zeros(nats, dtype=np.float64)
    vy = np.zeros(nats, dtype=np.float64)
    vmax = math.sqrt(3.0 * temp)
    sem = 462694.0

    for i in range(nats):
        sem = (16807.0 * sem) % 2147483647.0
        ran = sem / 2147483648.0
        vx[i] = vmax * (2.0 * ran - 1.0)
        sem = (16807.0 * sem) % 2147483647.0
        ran = sem / 2147483648.0
        vy[i] = vmax * (2.0 * ran - 1.0)

    if idum > 1 and elon > 0.0:
        for i in range(0, ntcm1 - 1, 2):
            drix = x[i] - x[i + 1]
            drvix = vx[i] - vx[i + 1]
            driy = y[i] - y[i + 1]
            drviy = vy[i] - vy[i + 1]
            gamai = (drvix * drix + drviy * driy) / (2.0 * elon * elon)
            vx[i] -= gamai * drix
            vx[i + 1] += gamai * drix
            vy[i] -= gamai * driy
            vy[i + 1] += gamai * driy

    total_mass = np.sum(amass[:nats])
    if total_mass > 0.0:
        vxm = np.sum(amass[:nats] * vx[:nats]) / total_mass
        vym = np.sum(amass[:nats] * vy[:nats]) / total_mass
        vx[:nats] -= vxm
        vy[:nats] -= vym

    return vx, vy


@njit(fastmath=True, cache=True)
def compute_energy_and_forces(
    nats, ncomp, x, y, fx, fy, lab, idum, ntcm1,
    box, rbox6, rbox12,
    rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
    lparedx, lparedy, epared, xmpared, xppared, ympared, yppared,
    rcparedx, rcparedy, fgyb,
    nl, nl2, lcv, ncv
):
    """Calculate potential energy, virial, wall forces/pressure, and atomic forces (ENERGY in Fortran)."""
    elj = 0.0
    vlj = 0.0
    uelj12 = 0.0
    uelj6 = 0.0
    uecaa = 0.0
    uecab = 0.0
    upared = 0.0
    ugrav = 0.0
    ppared = 0.0

    apared = 0.0
    fwl = 0.0
    fwr = 0.0
    fwu = 0.0
    fwd = 0.0
    nip = np.zeros(4, dtype=np.int32)
    nipy = 0

    if lparedx or lparedy:
        apared = (5.0 * ((5.0 / 2.0) ** (2.0 / 3.0)) / 3.0) * epared

    de13x = np.zeros(nats, dtype=np.float64)
    de13y = np.zeros(nats, dtype=np.float64)
    de7x = np.zeros(nats, dtype=np.float64)
    de7y = np.zeros(nats, dtype=np.float64)

    head = np.full(nl2, -1, dtype=np.int32)
    tail = np.full(nl2, -1, dtype=np.int32)
    lc = np.full(nats, -1, dtype=np.int32)

    for i in range(nats):
        if lparedx:
            # Clamp inside walls if numerical overshoot occurs
            if x[i] <= xmpared:
                x[i] = xmpared + 1e-6
            elif x[i] >= xppared:
                x[i] = xppared - 1e-6
            ix = int(nl * x[i])
            if ix < 0:
                ix = 0
            elif ix >= nl:
                ix = nl - 1
        else:
            x[i] = x[i] - math.floor(x[i])
            ix = int(nl * x[i]) % nl

        if lparedy:
            if y[i] <= ympared:
                y[i] = ympared + 1e-6
            elif y[i] >= yppared:
                y[i] = yppared - 1e-6
            iy = int(nl * y[i])
            if iy < 0:
                iy = 0
            elif iy >= nl:
                iy = nl - 1
        else:
            y[i] = y[i] - math.floor(y[i])
            iy = int(nl * y[i]) % nl

        ic = ix * nl + iy
        if head[ic] == -1:
            head[ic] = i
            tail[ic] = i
            lc[i] = -1
        else:
            it = tail[ic]
            lc[it] = i
            tail[ic] = i
            lc[i] = -1

        # Lateral wall interactions
        if lparedx:
            li = lab[i]  # 1, 2, or 3
            fx[i] = 0.0
            xim = box * (x[i] - xmpared)
            xiw = abs(xim)
            rcw = rcparedx[li - 1]
            if xiw < rcw:
                rxiw2 = 1.0 / (xiw * xiw)
                rxiw4 = rxiw2 * rxiw2
                rxiw6 = rxiw4 * rxiw2
                upi = apared * (rxiw4 * (rxiw6 - 1.0))
                nip[li] += 1
                upared += upi
                fxi = apared * (rxiw6 * (10.0 * rxiw6 - 4.0)) * xim
                fx[i] = fxi * box
                fwl += fxi

            xip = box * (x[i] - xppared)
            xiw = abs(xip)
            if xiw < rcw:
                rxiw2 = 1.0 / (xiw * xiw)
                rxiw4 = rxiw2 * rxiw2
                rxiw6 = rxiw4 * rxiw2
                upi = apared * (rxiw4 * (rxiw6 - 1.0))
                nip[li] += 1
                upared += upi
                fxi = apared * (rxiw6 * (10.0 * rxiw6 - 4.0)) * xip
                fx[i] += fxi * box
                fwr -= fxi
        else:
            fx[i] = 0.0

        # Horizontal wall interactions + gravity
        if lparedy:
            fy[i] = -fgyb
            ugrav += fgyb * y[i]
            yim = box * (y[i] - ympared)
            yiw = abs(yim)
            if yiw < rcparedy:
                rxiw2 = 1.0 / (yiw * yiw)
                rxiw4 = rxiw2 * rxiw2
                rxiw6 = rxiw4 * rxiw2
                upi = apared * (rxiw4 * (rxiw6 - 1.0))
                nipy += 1
                upared += upi
                fyi = apared * (rxiw6 * (10.0 * rxiw6 - 4.0)) * yim
                fy[i] = fyi * box
                fwd += fyi

            yip = box * (y[i] - yppared)
            yiw = abs(yip)
            if yiw < rcparedy:
                rxiw2 = 1.0 / (yiw * yiw)
                rxiw4 = rxiw2 * rxiw2
                rxiw6 = rxiw4 * rxiw2
                upi = apared * (rxiw4 * (rxiw6 - 1.0))
                nipy += 1
                upared += upi
                fyi = apared * (rxiw6 * (10.0 * rxiw6 - 4.0)) * yip
                fy[i] += fyi * box
                fwu -= fyi
        else:
            fy[i] = 0.0

    if lparedx:
        for c in range(1, ncomp + 1):
            rrcp = 1.0 / rcparedx[c - 1]
            ucpared = apared * ((rrcp ** 4) * ((rrcp ** 6) - 1.0))
            upared -= nip[c] * ucpared

    if lparedy:
        rrcp = 1.0 / rcparedy
        ucpared = apared * ((rrcp ** 4) * ((rrcp ** 6) - 1.0))
        upared -= nipy * ucpared

    if lparedx:
        if lparedy:
            ppared = (fwl + fwr + fwu + fwd) / (4.0 * box)
        else:
            ppared = (fwl + fwr) / (2.0 * box)
    elif lparedy:
        ppared = (fwu + fwd) / (2.0 * box)

    # Linked-cell pair interactions
    for ica in range(nl2):
        ia = head[ica]
        while ia != -1:
            for icv in range(ncv[ica]):
                icb = lcv[icv, ica]
                ib = head[icb]
                while ib != -1:
                    # Check if bonded dumbbell pair
                    is_unlinked = True
                    if idum > 1 and ia < ntcm1 and ib < ntcm1:
                        if (ia // 2) == (ib // 2):
                            is_unlinked = False

                    if is_unlinked:
                        wx = x[ia] - x[ib]
                        wy = y[ia] - y[ib]
                        if not lparedx:
                            wx -= math.floor(wx + 0.5)
                        if not lparedy:
                            wy -= math.floor(wy + 0.5)
                        rijsq = wx * wx + wy * wy

                        igual = lab[ia] == lab[ib]
                        loeq = igual and (rijsq <= rcaa2)
                        lodi = (not igual) and (rijsq <= rcab2)
                        if loeq or lodi:
                            rrij2 = 1.0 / rijsq
                            rrij6 = rrij2 * rrij2 * rrij2
                            rrij8 = rrij6 * rrij2
                            rrij12 = rrij6 * rrij6
                            rrij14 = rrij12 * rrij2

                            if loeq:
                                uecaa += 1.0
                            else:
                                uecab += 1.0

                            uelj6 += rrij6
                            uelj12 += rrij12

                            d7x = wx * rrij8
                            d13x = wx * rrij14
                            d7y = wy * rrij8
                            d13y = wy * rrij14

                            de7x[ia] += d7x
                            de7y[ia] += d7y
                            de7x[ib] -= d7x
                            de7y[ib] -= d7y

                            de13x[ia] += d13x
                            de13y[ia] += d13y
                            de13x[ib] -= d13x
                            de13y[ib] -= d13y

                    ib = lc[ib]
            ia = lc[ia]

        # Same-cell pair interactions
        ia = head[ica]
        while ia != -1:
            ib = lc[ia]
            while ib != -1:
                is_unlinked = True
                if idum > 1 and ia < ntcm1 and ib < ntcm1:
                    if (ia // 2) == (ib // 2):
                        is_unlinked = False

                if is_unlinked:
                    wx = x[ia] - x[ib]
                    wy = y[ia] - y[ib]
                    if not lparedx:
                        wx -= math.floor(wx + 0.5)
                    if not lparedy:
                        wy -= math.floor(wy + 0.5)
                    rijsq = wx * wx + wy * wy

                    igual = lab[ia] == lab[ib]
                    loeq = igual and (rijsq <= rcaa2)
                    lodi = (not igual) and (rijsq <= rcab2)
                    if loeq or lodi:
                        rrij2 = 1.0 / rijsq
                        rrij6 = rrij2 * rrij2 * rrij2
                        rrij8 = rrij6 * rrij2
                        rrij12 = rrij6 * rrij6
                        rrij14 = rrij12 * rrij2

                        if loeq:
                            uecaa += 1.0
                        else:
                            uecab += 1.0

                        uelj6 += rrij6
                        uelj12 += rrij12

                        d7x = wx * rrij8
                        d13x = wx * rrij14
                        d7y = wy * rrij8
                        d13y = wy * rrij14

                        de7x[ia] += d7x
                        de7y[ia] += d7y
                        de7x[ib] -= d7x
                        de7y[ib] -= d7y

                        de13x[ia] += d13x
                        de13y[ia] += d13y
                        de13x[ib] -= d13x
                        de13y[ib] -= d13y

                ib = lc[ib]
            ia = lc[ia]

    elj = 4.0 * rbox12 * uelj12 - 4.0 * rbox6 * uelj6
    vlj = 24.0 * rbox12 * uelj12 - 12.0 * rbox6 * uelj6
    elj = (
        elj
        - 4.0 * uecaa * (rbox12 / rcaa12 - rbox6 / rcaa6)
        - 4.0 * uecab * (rbox12 / rcab12 - rbox6 / rcab6)
    )

    for i in range(nats):
        fx_lj = 48.0 * rbox12 * de13x[i] - 24.0 * rbox6 * de7x[i]
        fy_lj = 48.0 * rbox12 * de13y[i] - 24.0 * rbox6 * de7y[i]
        if lparedx:
            fx[i] += fx_lj
        else:
            fx[i] = fx_lj
        if lparedy:
            fy[i] += fy_lj
        else:
            fy[i] = fy_lj

    return elj, vlj, upared, ugrav, ppared


@njit(fastmath=True, cache=True)
def step_batch_numba(
    ncb, nats, ncomp, x, y, vx, vy, fx, fy, amass, lab,
    idum, ntcm, elon, tistep, temp, dens, scaflg, frscal,
    box, rbox, rbox6, rbox12,
    rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
    lparedx, lparedy, epared, xmpared, xppared, ympared, yppared,
    rcparedx, rcparedy, fgyb,
    nl, nl2, lcv, ncv,
    dri, ngm, ngmy, iheter,
    igaa, igab, igw1, igw2, igw3, igwy, nprom,
    graa, grab, gw1, gw2, gw3, gwy,
    xxx, yyy, iax, iay, isize, irsize
):
    """
    Execute `ncb` Velocity-Verlet / RATTLE steps and update RDF & density histograms
    (equivalent to a full call to `mdmezcla` in Fortran).
    """
    ntcm1 = ntcm[1]
    lpared = lparedx or lparedy
    vbox = float(nats) / dens
    pideal = float(nats - 1) * dens / float(nats)

    # Initial force calculation before the ncb loop (inoe == 0)
    elj, vlj, upared, ugrav, ppared = compute_energy_and_forces(
        nats, ncomp, x, y, fx, fy, lab, idum, ntcm1,
        box, rbox6, rbox12,
        rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
        lparedx, lparedy, epared, xmpared, xppared, ympared, yppared,
        rcparedx, rcparedy, fgyb,
        nl, nl2, lcv, ncv
    )

    qx = np.zeros(nats, dtype=np.float64)
    qy = np.zeros(nats, dtype=np.float64)
    x1 = np.zeros(nats, dtype=np.float64)
    y1 = np.zeros(nats, dtype=np.float64)
    vx1 = np.zeros(nats, dtype=np.float64)
    vy1 = np.zeros(nats, dtype=np.float64)

    tol = 10.0 * tistep * tistep
    elon2 = elon * elon
    itmax = 5

    spres = 0.0

    for ic_step in range(1, ncb + 1):
        # MOVES2: 2-step velocity Verlet / RATTLE
        if idum == 1:
            for i in range(nats):
                qx[i] = vx[i] + 0.5 * fx[i] * tistep
                qy[i] = vy[i] + 0.5 * fy[i] * tistep
                x[i] += tistep * qx[i]
                y[i] += tistep * qy[i]

            elj, vlj, upared, ugrav, ppared = compute_energy_and_forces(
                nats, ncomp, x, y, fx, fy, lab, idum, ntcm1,
                box, rbox6, rbox12,
                rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
                lparedx, lparedy, epared, xmpared, xppared, ympared, yppared,
                rcparedx, rcparedy, fgyb,
                nl, nl2, lcv, ncv
            )

            for i in range(nats):
                vx[i] = qx[i] + 0.5 * fx[i] * tistep
                vy[i] = qy[i] + 0.5 * fy[i] * tistep
        else:
            # First half-step with RATTLE position constraints for dumbbells (0 .. ntcm1-1)
            for i in range(0, ntcm1 - 1, 2):
                drix = x[i] - x[i + 1]
                driy = y[i] - y[i + 1]
                if not lparedx:
                    drix -= math.floor(drix + 0.5)
                if not lparedy:
                    driy -= math.floor(driy + 0.5)

                gamai = 0.0
                err = 1000.0
                it = 0
                inv_m_sum = (1.0 / amass[i]) + (1.0 / amass[i + 1])
                while err > tol and it < itmax:
                    it += 1
                    qx[i] = vx[i] + 0.5 * fx[i] * tistep - gamai * drix / amass[i]
                    qy[i] = vy[i] + 0.5 * fy[i] * tistep - gamai * driy / amass[i]
                    qx[i + 1] = vx[i + 1] + 0.5 * fx[i + 1] * tistep + gamai * drix / amass[i]
                    qy[i + 1] = vy[i + 1] + 0.5 * fy[i + 1] * tistep + gamai * driy / amass[i]

                    x1[i] = x[i] + tistep * qx[i]
                    x1[i + 1] = x[i + 1] + tistep * qx[i + 1]
                    dripx = x1[i] - x1[i + 1]

                    y1[i] = y[i] + tistep * qy[i]
                    y1[i + 1] = y[i + 1] + tistep * qy[i + 1]
                    dripy = y1[i] - y1[i + 1]

                    if not lparedx:
                        dripx -= math.floor(dripx + 0.5)
                    if not lparedy:
                        dripy -= math.floor(dripy + 0.5)

                    diff2 = dripx * dripx + dripy * dripy - elon2
                    denom = 2.0 * tistep * (drix * dripx + driy * dripy) * inv_m_sum
                    if abs(denom) > 1e-18:
                        gamai = diff2 / denom
                    err = abs(diff2)

            for i in range(ntcm1, nats):
                qx[i] = vx[i] + 0.5 * fx[i] * tistep
                qy[i] = vy[i] + 0.5 * fy[i] * tistep
                x[i] += tistep * qx[i]
                y[i] += tistep * qy[i]

            for i in range(ntcm1):
                x[i] = x1[i]
                y[i] = y1[i]

            elj, vlj, upared, ugrav, ppared = compute_energy_and_forces(
                nats, ncomp, x, y, fx, fy, lab, idum, ntcm1,
                box, rbox6, rbox12,
                rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
                lparedx, lparedy, epared, xmpared, xppared, ympared, yppared,
                rcparedx, rcparedy, fgyb,
                nl, nl2, lcv, ncv
            )

            # Second half-step with RATTLE velocity constraints
            for i in range(0, ntcm1 - 1, 2):
                drix = x[i] - x[i + 1]
                driy = y[i] - y[i + 1]
                if not lparedx:
                    drix -= math.floor(drix + 0.5)
                if not lparedy:
                    driy -= math.floor(driy + 0.5)

                gamai = 0.0
                err = 1000.0
                it = 0
                inv_m_sum = (1.0 / amass[i]) + (1.0 / amass[i + 1])
                while err > tol and it < itmax:
                    it += 1
                    vx1[i] = qx[i] + 0.5 * fx[i] * tistep - gamai * drix / amass[i]
                    vx1[i + 1] = qx[i + 1] + 0.5 * fx[i + 1] * tistep + gamai * drix / amass[i + 1]
                    drvix = vx1[i] - vx1[i + 1]

                    vy1[i] = qy[i] + 0.5 * fy[i] * tistep - gamai * driy / amass[i]
                    vy1[i + 1] = qy[i + 1] + 0.5 * fy[i + 1] * tistep + gamai * driy / amass[i + 1]
                    drviy = vy1[i] - vy1[i + 1]

                    diff2 = drvix * drix + drviy * driy
                    err = abs(diff2)
                    gamai = diff2 / (elon2 * inv_m_sum)

            for i in range(ntcm1, nats):
                vx[i] = qx[i] + 0.5 * fx[i] * tistep
                vy[i] = qy[i] + 0.5 * fy[i] * tistep

            for i in range(ntcm1):
                vx[i] = vx1[i]
                vy[i] = vy1[i]

        # KINETE2
        sv2 = 0.0
        for i in range(nats):
            sv2 += amass[i] * (vx[i] * vx[i] + vy[i] * vy[i])
        ekinet = 0.5 * sv2
        if idum == 1:
            tatom = ekinet / float(nats - 1)
        else:
            tatom = ekinet / float(nats - 1 - ntcm1 // 2)

        if lpared:
            presio = ppared
        else:
            presio = pideal * tatom + vlj / vbox

        spres += presio

        # Velocity rescaling thermostat (ESCALE)
        if scaflg > 0.0 and (ic_step % frscal == 0):
            if idum == 1:
                trun = sv2 / float(2 * nats - 2)
            else:
                trun = sv2 / float(2 * nats - 2 - ntcm1 // 2)
            if trun > 1e-15:
                esc = 1.0 + scaflg * (math.sqrt(temp / trun) - 1.0)
                for i in range(nats):
                    vx[i] *= esc
                    vy[i] *= esc

    pres = spres / float(ncb)

    # Accumulate wall density profiles
    nhp = ngm
    if lpared:
        if lparedx:
            dr_wx = (xppared - xmpared) / float(nhp)
            for i in range(nats):
                ic = int((x[i] - xmpared) / dr_wx)
                if 1 <= ic < NGMX:
                    li = lab[i]
                    if li == 1:
                        igw1[ic] += 1.0
                    elif li == 2:
                        igw2[ic] += 1.0
                    elif li == 3:
                        igw3[ic] += 1.0
        if lparedy:
            dr_wy = (yppared - ympared) / float(nhp)
            for i in range(nats):
                ic = int((y[i] - ympared) / dr_wy)
                if 1 <= ic < NGMX:
                    igwy[ic] += 1.0

    # Accumulate radial distribution function g(r)
    dr = dri / box
    for i in range(nats):
        xi = x[i]
        yi = y[i]
        li = lab[i]
        for j in range(i + 1, nats):
            dx = xi - x[j]
            dy = yi - y[j]
            if not lparedx:
                dx -= math.floor(dx + 0.5)
            if not lparedy:
                dy -= math.floor(dy + 0.5)
            rr = math.sqrt(dx * dx + dy * dy)
            if rr < 0.5:
                ic = int(math.floor(rr / dr + 0.5))
                if 1 <= ic <= ngm and ic < NGMX:
                    if li == lab[j]:
                        igaa[ic] += 1.0
                    else:
                        igab[ic] += 1.0

    # Output coordinates & screen pixel positions (1-based index i+1 to match Java arrays)
    offset = (irsize - isize) // 2
    for i in range(nats):
        if lparedx:
            xx = x[i] - 0.5
        else:
            xx = x[i] - math.floor(x[i] + 0.5)
        if lparedy:
            yy = y[i] - 0.5
        else:
            yy = y[i] - math.floor(y[i] + 0.5)
        xxx[i + 1] = xx
        yyy[i + 1] = yy
        iax[i + 1] = int((xx + 0.5) * isize + 0.5 + offset)
        iay[i + 1] = int((yy + 0.5) * isize + 0.5 + offset)

    nprom += 1
    ndmax = min(int(math.floor(0.5 / dr + 0.5)), ngm)
    cte = 0.5 * (dens * float(nprom)) / float(nats)

    max_gaa = 0.0
    max_gab = 0.0
    n1 = float(ntcm[1])
    n2 = float(ntcm[2])
    fnats = float(nats)

    for i in range(1, ndmax + 1):
        r_out = (i + 0.5) * dr
        r_in = (i - 0.5) * dr
        ds = PI * (r_out * r_out - r_in * r_in) * box * box
        rnideal = 1.0
        rnidealab = 1.0

        if iheter == 0:
            if ncomp == 1:
                rnideal = ds * cte * fnats * fnats
            elif ncomp == 2:
                rnideal = ds * cte * (n1 * n1 + (fnats - n1) ** 2)
                rnidealab = ds * cte * n1 * (fnats - n1)
            elif ncomp == 3:
                rnideal = ds * cte * (n1 * n1 + (n2 - n1) ** 2 + (fnats - n2) ** 2)
                rnidealab = ds * cte * (
                    n1 * (fnats - n2) + n1 * (n2 - n1) + (n2 - n1) * (fnats - n2)
                )
        else:
            if ncomp == 1:
                rnideal = ds * cte * fnats * fnats / 2.0
                val_ab = igab[i] / rnideal
                if val_ab > 6.0:
                    val_ab = 6.0
                grab[i] = val_ab
                if val_ab > max_gab:
                    max_gab = val_ab
            elif ncomp == 2:
                rnideal = ds * cte * (n1 * n1 / 4.0 + (fnats - n1 / 2.0) ** 2)
                rnidealab = ds * cte * (n1 / 2.0) * (fnats - n1 / 2.0)
            elif ncomp == 3:
                rnideal = ds * cte * (
                    (n1 / 2.0 + fnats - n2) ** 2 + (n1 / 2.0 + n2 - n1) ** 2
                )
                rnidealab = ds * cte * (
                    (n1 / 2.0 + fnats - n2) * (n1 / 2.0 + n2 - n1)
                )

        val_aa = igaa[i] / rnideal if rnideal > 0.0 else 0.0
        if val_aa > 6.0:
            val_aa = 6.0
        graa[i] = val_aa
        if val_aa > max_gaa:
            max_gaa = val_aa

        if ncomp > 1:
            val_ab = igab[i] / (2.0 * rnidealab) if rnidealab > 0.0 else 0.0
            if val_ab > 6.0:
                val_ab = 6.0
            grab[i] = val_ab
            if val_ab > max_gab:
                max_gab = val_ab

    graa[0] = max_gaa
    if ncomp > 1 or iheter == 1:
        grab[0] = max_gab
        if max_gab > graa[0]:
            graa[0] = max_gab

    if lpared:
        if lparedx:
            dr_wx = (xppared - xmpared) / float(nhp)
            vc = dr_wx * box * box
            cn = 1.0 / (float(nprom) * vc)
            m1 = 0.0
            m2 = 0.0
            for i in range(1, ngm):
                gw1[i] = cn * igw1[i]
                gw2[i] = cn * igw2[i]
                gw3[i] = cn * igw3[i]
                if gw1[i] > m1:
                    m1 = gw1[i]
                if gw2[i] > m2:
                    m2 = gw2[i]
            gw2[0] = m2
            gw1[0] = max(m1, m2)

        if lparedy:
            dr_wy = (yppared - ympared) / float(nhp)
            vc = dr_wy * box * box
            cn = 1.0 / (float(nprom) * vc)
            my_max = 0.0
            for i in range(1, ngmy):
                gwy[i] = cn * igwy[i]
                if gwy[i] > my_max:
                    my_max = gwy[i]
            gwy[0] = my_max

    return pres, nprom


class MDEngine:
    """Stateful wrapper around the Numba-accelerated 2D MD engine."""

    def __init__(self):
        self.x = np.zeros(MATS, dtype=np.float64)
        self.y = np.zeros(MATS, dtype=np.float64)
        self.vx = np.zeros(MATS, dtype=np.float64)
        self.vy = np.zeros(MATS, dtype=np.float64)
        self.fx = np.zeros(MATS, dtype=np.float64)
        self.fy = np.zeros(MATS, dtype=np.float64)
        self.amass = np.ones(MATS, dtype=np.float64)
        self.lab = np.ones(MATS, dtype=np.int32)

        self.igaa = np.zeros(NGMX, dtype=np.float64)
        self.igab = np.zeros(NGMX, dtype=np.float64)
        self.igw1 = np.zeros(NGMX, dtype=np.float64)
        self.igw2 = np.zeros(NGMX, dtype=np.float64)
        self.igw3 = np.zeros(NGMX, dtype=np.float64)
        self.igwy = np.zeros(NGMX, dtype=np.float64)

        self.nprom = 0
        self.tr_old = -1.0
        self.ra_old = -1.0
        self.rb_old = -1.0
        self.nc_old = -1

    def jsimul(
        self,
        iax, iay, xxx, yyy,
        graa, grab,
        dri, tstep, rci, rcd,
        temp, densid, scaflg, frscal,
        ncomp, nimp, ngm, ngmy,
        isize, irsize, inic, ncb,
        gw1, gw2, gw3, gwy,
        rcw1, rcw2, rcw3, rcwy, epsw,
        iparedx, iparedy, fgy,
        natcm, idumb, elong, iheter
    ):
        """
        Drop-in replacement for the legacy JNI `Simulation.jsimul` -> Fortran `mdmezcla` call.

        Parameters
        ----------
        iax, iay : np.ndarray (int32, length NMOL+1)
            Output 1-based screen pixel coordinates for rendering particles.
        xxx, yyy : np.ndarray (float64, length NMOL+1)
            Output 1-based reduced coordinates in [-0.5, 0.5].
        graa, grab : np.ndarray (float64, length NGM+1)
            Output radial distribution functions g_AA(r) and g_AB(r); index 0 stores max peak.
        dri : float
            Radial histogram bin width in reduced LJ units.
        tstep : float
            Integration time step in reduced units.
        rci, rcd : float
            Cutoff distances for like (A-A, B-B, C-C) and unlike (A-B, A-C, B-C) interactions.
        temp, densid : float
            Target reduced temperature T* and number density rho*.
        scaflg : float
            Berendsen/relaxation velocity-rescaling coupling factor (>0 active, <0 NVE).
        frscal : int
            Thermostat rescaling frequency in MD steps.
        ncomp : int
            Number of chemical components (1 = pure, 2 = binary, 3 = ternary).
        nimp : int
            Total particle array size including 1-based offset (`nats = nimp - 1`).
        ngm, ngmy : int
            Number of histogram bins for g(r) / rho(x) and vertical profile rho(y).
        isize, irsize : int
            Inner simulation box pixel size and total canvas pixel size (including walls).
        inic : int
            Initialization flag (0 = rebuild lattice & velocities, 1 = continue, 3 = quench v=0).
        ncb : int
            Number of MD integration steps per visualization frame.
        gw1, gw2, gw3, gwy : np.ndarray (float64)
            Output lateral density profiles for species 1..3 and vertical total density profile.
        rcw1, rcw2, rcw3, rcwy : float
            Wall interaction cutoff distances for species 1..3 (lateral) and horizontal walls.
        epsw : float
            Wall-particle potential well depth parameter.
        iparedx, iparedy : int
            Flags (0 or 1) enabling lateral (x) and horizontal (y) confining walls.
        fgy : float
            External downward gravitational acceleration parameter.
        natcm : np.ndarray (int32, length 5)
            Cumulative particle indices per component (`0, N_1, N_1+N_2, N_total`).
        idumb : int
            1 for monomeric Component A, 2 for diatomic dumbbell Component A.
        elong : float
            Dumbbell bond length in reduced units.
        iheter : int
            0 for homonuclear dumbbells, 1 for heteronuclear dumbbells.

        Returns
        -------
        pres : float
            Time-averaged reduced pressure P* over the `ncb` integration steps.
        """
        nats = nimp - 1
        nats = ncomp * (nats // ncomp)

        lparedx = iparedx == 1
        lparedy = iparedy == 1

        ntcm = np.zeros(5, dtype=np.int32)
        for k in range(1, ncomp + 1):
            ntcm[k] = natcm[k]
        if ncomp == 1:
            ntcm[1] = nats

        vbox = float(nats) / densid
        box = math.sqrt(vbox)
        rbox = 1.0 / box
        rbox2 = rbox * rbox
        rbox6 = rbox2 * rbox2 * rbox2
        rbox12 = rbox6 * rbox6

        rcoff = max(rci, rcd)
        nl = int(box / rcoff)
        nl, nl2, lcv, ncv = build_neighbor_cells(nl, lparedx, lparedy)

        xmpared = -rbox if lparedx else 0.0
        xppared = 1.0 + rbox if lparedx else 1.0
        ympared = -rbox if lparedy else 0.0
        yppared = 1.0 + rbox if lparedy else 1.0
        rcparedx = np.array([rcw1, rcw2, rcw3], dtype=np.float64)
        rcparedy = rcwy
        fgyb = fgy * box if lparedy else 0.0

        tistep = tstep * rbox
        elon = elong * rbox

        rcaa = rci * rbox
        rcaa2 = rcaa * rcaa
        rcaa6 = rcaa2 * rcaa2 * rcaa2
        rcaa12 = rcaa6 * rcaa6

        rcab = rcd * rbox
        rcab2 = rcab * rcab
        rcab6 = rcab2 * rcab2 * rcab2
        rcab12 = rcab6 * rcab6

        # Assign particle species labels and masses
        for j in range(1, ncomp + 1):
            for i in range(ntcm[j - 1], min(ntcm[j], nats)):
                lbl = j
                if iheter != 0 and j == 1:
                    lbl = (i % 2) + 2
                self.lab[i] = lbl
                if abs(fgy) > 1e-4:
                    self.amass[i] = 1.0 if lbl == 1 else 2.0
                else:
                    self.amass[i] = 1.0

        inicia = False
        if inic == 0:
            self.tr_old = temp
            self.ra_old = rci
            self.rb_old = rcd
            self.nc_old = ncomp
            x_init, y_init = init_lattice(nats, idumb, ntcm[1], elon)
            self.x[:nats] = x_init
            self.y[:nats] = y_init
            vx_init, vy_init = init_velocities(
                nats, temp, idumb, ntcm[1], elon, self.x, self.y, self.amass
            )
            self.vx[:nats] = vx_init
            self.vy[:nats] = vy_init
            inicia = True
        else:
            if inic == 3:
                self.vx[:nats] = 0.0
                self.vy[:nats] = 0.0
            if (
                abs(self.tr_old - temp) > 1e-4
                or abs(self.ra_old - rci) > 1e-4
                or abs(self.rb_old - rcd) > 1e-4
                or self.nc_old != ncomp
            ):
                inicia = True

        if inicia:
            self.nprom = 0
            self.igaa.fill(0.0)
            self.igab.fill(0.0)
            self.igw1.fill(0.0)
            self.igw2.fill(0.0)
            self.igw3.fill(0.0)
            self.igwy.fill(0.0)
            graa.fill(0.0)
            grab.fill(0.0)
            gw1.fill(0.0)
            gw2.fill(0.0)
            gw3.fill(0.0)
            gwy.fill(0.0)

        pres, self.nprom = step_batch_numba(
            ncb, nats, ncomp,
            self.x, self.y, self.vx, self.vy, self.fx, self.fy, self.amass, self.lab,
            idumb, ntcm, elon, tistep, temp, densid, scaflg, frscal,
            box, rbox, rbox6, rbox12,
            rcaa2, rcaa6, rcaa12, rcab2, rcab6, rcab12,
            lparedx, lparedy, epsw, xmpared, xppared, ympared, yppared,
            rcparedx, rcparedy, fgyb,
            nl, nl2, lcv, ncv,
            dri, ngm, ngmy, iheter,
            self.igaa, self.igab, self.igw1, self.igw2, self.igw3, self.igwy, self.nprom,
            graa, grab, gw1, gw2, gw3, gwy,
            xxx, yyy, iax, iay, isize, irsize
        )

        self.nc_old = ncomp
        self.ra_old = rci
        self.rb_old = rcd
        self.tr_old = temp

        return pres
