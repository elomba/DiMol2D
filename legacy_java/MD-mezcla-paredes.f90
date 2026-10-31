! *************************************************************
! *************************************************************
      program mdmix
! *************************************************************
!  Interaccion con la pared:
!     U = 3.0700*epsw(1./(x-x_w)**10-1./(x-x_w)**6)
! *************************************************************
      Use Comun
      Use Coordenadas
      Implicit None
      Integer (kind=4) :: iax(0:mats-1), iay(0:mats-1)
      Real (kind=8) :: xx(0:mats-1), yy(0:mats-1)
!     Real (kind=8) :: vx(0:mats-1), vy(0:mats-1)
      Integer (kind=4) :: gi(0:ngmx), gd(0:ngmx)
      Integer (kind=4) :: nats, nimp
      Integer (kind=4) :: iparedx, iparedy, ncomp, natcm(0:3)
      Integer (kind=4) :: frscal
      Integer (kind=4) :: ngm,ngmy,  nhp
      Integer (kind=4) :: ib, nbe, nbp, ncb, ix, i
      Integer (kind=4) :: inic
      Integer (kind=4) :: irsize, isize, idum
      Real (kind=8) :: gw1(0:ngmx), gw2(0:ngmx), gw3(0:ngmx), gwy(0:ngmx)
      Real (kind=8) :: scaflg, tstep
      Real (kind=8) :: temp, dens, epsw, pres
      Real (kind=8) :: rci, rcd, rcw1, rcw2, rcw3, rcwy, fgy
      Real (kind=8) :: dri
      Real (kind=8) :: box, xwm, width, dxw, vc
      Real (kind=8) :: xil, xir, pdx1, pdx2, pdx3, yid, yiu, pdy, elon
       
      open(1,file='pared.datos',status='unknown')
      open(2,file='CONF',status='unknown',form='unformatted')
      read(1,*) inic, nats
!
!  iparedx=0 --> Sin paredes -x-
!  iparedy=0 --> Sin paredes -y-
!
      Read(1,*) iparedx, iparedy, ncomp, idum, elon
      read(1,*) temp, dens
      read(1,*) rci, rcd
      read(1,*) tstep, scaflg, frscal
      read(1,*)  ncb 
!
      read(1,*) ngm, dri
      read(1,*) epsw, rcw1, rcw2, rcw3
      read(1,*) fgy
      rcwy = 2.5d0**(1.d0/6.d0)
!
      if ( inic.ne.0 ) then
        rewind 2
        read(2) (x(i),I=1,nats)
        read(2) (y(i),I=1,nats)
        read(2) (vx(i),I=1,nats)
        read(2) (vy(i),I=1,nats)
      end if
      natcm(0) = 0
      natcm(1) = nats 
      nimp = nats+1
!
      box = sqrt ( dble(nats)/dens )
      xwm = -1.d0
      width = 2.d0+box
      nhp = ngm
      ngmy = ngm
      dxw = width/dble(nhp)
!
!     call mdmezcla (iax(0),iay(0),x(0),y(0),gi(0),gd(0), &
      Call mdmezcla (iax(0),iay(0),xx(0),yy(0),gi(0),gd(0), &
   &            pres, dri, tstep, rci, rcd, temp, dens, &
   &            SCAFLG, FRSCAL, ncomp, nimp, ngm, ngmy, &
   &            isize, irsize, INIC, ncb, &
   &            gw1(0), gw2(0), gw3(0),  gwy(0), &
   &            rcw1, rcw2, rcw3, rcwy, epsw, iparedx, iparedy, fgy, natcm, idum, elon ) 
!
        rewind 2
        write(2) (xx(i),I=1,nats)
        write(2) (yy(i),I=1,nats)
        write(2) (vx(i),I=1,nats)
        write(2) (vy(i),I=1,nats)
       if ( iparedx.eq.1 ) then
         rewind 31
         do ix=0,ngm-1
            xil= -1.d0+ix*dxw+dxw/2.d0 
            xir= xil+dxw
           write(31,3101) xil, gw1(ix), gw2(ix), gw3(ix)
           write(31,3101) xir, gw1(ix), gw2(ix), gw3(ix)
         end do
         close (31)
       end if
       if ( iparedy.eq.1 ) then
         rewind 32
         do ix=0,ngm-1
           yid= -1.d0+ix*dxw+dxw/2.d0 
           yiu= yid+dxw
           write(32,3101) yid, gwy(ix)
           write(32,3101) yiu, gwy(ix)
         end do
         close (32)
       end if
3101  format(1x,4(f15.6,1x) )
      end
