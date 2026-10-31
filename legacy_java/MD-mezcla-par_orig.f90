 
   Module Comun
     Integer (kind=4) , Parameter :: MATS = 4000
     Integer (kind=4) , Parameter :: NLMX =   50
     Integer (kind=4) , Parameter :: NCMX = NLMX*NLMX
     Integer (kind=4) , Parameter :: NGMX = 5000 
     Real (kind=8) :: PI = 3.1415926535898d0
   End Module Comun

   Module Ener_vir
     Real (kind=8) :: ELJ , EKINET , VLJ, Upared, Ugrav, Ppared
   End Module Ener_vir
     
   Module Sumas
     Real (kind=8) :: SELJ , SKINET ,SETOTA,STATOM, SPRES, SPRESM, supar, sugrav
   End Module Sumas

   Module Potencial
     Real (kind=8) :: rcaa2 , rcaa6 , rcaa12 , rcab2 , rcab6 , rcab12
! ------------------------------------------------------
     Real (kind=8) :: epared, xmpared, xppared, ympared, yppared
     Real (kind=8) :: rcparedx(3), rcparedy, fgyb
     Logical :: Lpared, Lparedx, Lparedy
   End Module Potencial

   Module Aleatoria
     Real (kind=8) :: SEM
   End Module Aleatoria

   Module Coordenadas
     Use Comun
     Real (kind=8) , Dimension (MATS) :: X , Y , VX , VY , X1 , Y1 , VX1 , VY1, Amass
     Integer,  Dimension (MATS) :: New
   End Module Coordenadas



   Module Labels
     Use Comun
     Integer (kind=4) , Dimension (MATS) :: lab
     Integer (kind=4) , Dimension (4,0:NCMX-1) :: lcv
     Integer (kind=4) , Dimension (0:NCMX-1) :: ncv
     Integer (kind=4) :: nl , nl2, nprom
     Real (kind=8) , Dimension (NGMX) :: igaa, igab, igw1, igw2, igw3, igwy
   End Module Labels

   Module Datos
     Real (kind=8) :: TISTEP 
     Integer (kind=4) :: NATS , ipaso
   End Module Datos

   Module Fuerza
     Use Comun
     Real (kind=8) , Dimension (MATS) :: FX , FY
   End Module Fuerza

   Module Caja
     Real (kind=8) :: BOX , RBOX , RBOX2 , RBOX6 , RBOX12
   End Module Caja
! *************************************************************
! ------------------------------------------------------------------------------
    Subroutine mdmezcla( iax, iay, xxx, yyy, graa, grab, &
   &                     pres, dri, tstep , rci , rcd, &
   &     temp, dens, SCAFLG, FRSCAL, ncomp, nimp, ngm, ngmy, &
   &     isize, irsize, INIC, ncb,gw1,gw2,gw3,gwy,rcw1,rcw2,rcw3, &
   &     rcwy, epsw,iparedx, iparedy, fgy, natcm) 
! ------------------------------------------------------------------------------
! ------------------------------------------------------------------------------


      Use Comun
      Use Ener_vir
      Use Sumas
      Use Potencial
      Use Aleatoria
      Use Coordenadas
      Use Labels
      Use Datos
      Use Caja

      Implicit None

      Integer (kind=4) :: nimp, isize, irsize, ncomp , &
           &   ngm , INIC , i , ib , ic , j , ndmax , ncb , FRSCAL
      Integer (kind=4) :: iax(0:nimp-1), iay(0:nimp-1), natcm(0:ncomp)
      Integer (kind=4) , Save :: nc_old
! ...................................................................
! ...................................................................
!   nga: Variables a incorporar en la llamada:
!         ipared, 1 (Pared)
!         rcpared: Radio de corte interacciones pared-particula
!         Pared: Posicion: -sigma, L + sigma
!        
      Integer (kind=4) :: iparedx, iparedy, ngmy
      Integer (kind=4) :: nhp
! ...................................................................
! ...................................................................
      Real (kind=8), Save :: tr_old , ra_old , rb_old
      Real (kind=8) :: xxx(0:nimp-1), yyy(0:nimp-1), graa(0:ngm-1) , &
           &       grab(0:ngm-1) , tstep , dens , rci , rcd , cte , &
           &       temp , RCOFF , PIDEAL , R , RR , dr , &
           &       VBOX , tatom , dri , dx , dy , xi , yi , ds , rnideal , &
           &       presio , etotal   , enkt , etot , pres , temat , scaflg 
      Real (kind=8) :: gw1(0:ngm-1), gw2(0:ngm-1), gw3(0:ngm-1)
      Real (kind=8) :: gwy(0:ngmy-1)
      Real (kind=8) :: rcaa, rcab
      Real (kind=8) :: drw, cn, vc
      Real (kind=8) :: rcwy, fgy 
      Real (kind=8) :: epsw, rcw1, rcw2, rcw3, upar
      Logical, Save :: Inicia 
! ...................................................................
! .                                                                 .
! .       Dinamica Molecular (2d Interaccion LJ)                    .
! .        NVE: LJ truncado y desplazado                            .
! .                                                                 .
! .        [MEZCLA (simetrica, equimolecular) hasta tres componentes
! .         A-A, B-B; A-B. ternaria                                 .
! .         Diferencia en la interaccion (cutoff)                   .
! .                                                                 .
! ...................................................................
!
!
      natcm(0) = 0
!      Print *, natcm(0:ncomp)
      nhp = ngm
      Lparedx = iparedx.eq.1 
      Lparedy = iparedy.eq.1 
      Lpared = Lparedx .or. Lparedy
!
!   control de vectores
!
      If( .not. Lpared  ) then
       if ( ngm > NGMX) Then
         write(*,*) 'Ngm= ', Ngm
         Stop " **** Ngm > NGMX (5000) **** "
       end if
      End If

      If(nimp > MATS) Then
         write(*,*) 'Nimp= ', Nimp
         Stop " **** Nimp > MATS (4000) **** "
      End If

      If(INIC == 0) Then
         Inicia = .False.
         tr_old = temp
         ra_old = rcaa
         rb_old = rcab
         nc_old = ncomp
      End If

      If(Abs(tr_old-temp) > 0.00010d0) Then
         Inicia = .True. 
      Else If(Abs(ra_old-rci) > 0.00010d0) Then
         Inicia = .True. 
      Else If(Abs(rb_old-rcd) > 0.00010d0) Then
         Inicia = .True. 
      Else If(nc_old /= ncomp) Then
         Inicia = .True. 
      Endif

      If(Inicia) Then
         nprom   = 0
         SPRESM = 0.d0
         if ( Lparedx ) then
           igw1(:) = 0.d0
           igw2(:) = 0.d0
           igw3(:) = 0.d0
         end if
         if ( Lparedy ) then
           igwy(:) = 0.d0
         end if
         igaa(:) = 0.d0
         igab(:) = 0.d0
      End If

      ipaso = ipaso + 1
      NATS  = nimp - 1
      sem    = 462694.0d0
      tistep = tstep
      epared = epsw

!  ##########  Mismo numero de particulas de cada componente

      NATS = ncomp*(NATS/ncomp)

! ..........................................................
!     RCOF2  =  RADIO DE CORTE DEL POTENCIAL AL CUADRADO
!     VBOX   =  VOLUMEN DE LA CAJA ( EN UNIDADES REDUCIDAS )
!     BOX   =  LONGITUD DE LA CAJA ( EN UNIDADES REDUCIDAS )
!     RBOX = BOX ** -1
! ..........................................................
      VBOX   = Dble(NATS) / DENS
      BOX    = Sqrt( VBOX )
      RBOX   = 1.00d0 / BOX
      RBOX2  = RBOX * RBOX
      RBOX6  = RBOX2 * RBOX2 * RBOX2
      RBOX12 = RBOX6 * RBOX6

      rcaa = rci
      rcab = rcd

      RCOFF = Max( rcaa , rcab)
      nl    = Int(box/RCOFF)
      nl2   = nl*nl
      Call Liscv ( Lparedx, Lparedy  ) 
!
! *** COORDENADAS DE LAS PAREDES
!    Pared situada en (-sigma, box+sigma)
!
       if ( Lparedx ) then
         xmpared = -rbox
         xppared = 1.d0+rbox
         rcparedx(1) = rcw1
         rcparedx(2) = rcw2
         rcparedx(3) = rcw3
       end if
       if ( Lparedy ) then
         ympared = -rbox
         yppared = 1.d0+rbox
         rcparedy = rcwy
         fgyb = fgy*box
       end if
! 

!  *** SCALING TIME-STEP, R-CUT-OFF, RIJEQ, DKRIJ

      RCOFF  = RCOFF * RBOX
      TISTEP = TISTEP * RBOX

      If(nl > NLMX) Stop " *** nl > NLMX (50) *** "

      If(RCOFF > 0.500d0) Then
         Stop " *** Alcance excesivo del potencial *** "
      End If

      rcaa   = rcaa*rbox
      rcaa2  = rcaa*rcaa
      rcaa6  = rcaa2 * rcaa2 * rcaa2
      rcaa12 = rcaa6 * rcaa6 
      rcab   = rcab*rbox
      rcab2  = rcab*rcab
      rcab6  = rcab2 * rcab2 * rcab2
      rcab12 = rcab6 * rcab6 

!    COEFFICIENTS TO CALCULATE PRESSURE, AND CUT-OFF CORRECTIONS

      PIDEAL = Dble ( NATS-1)* DENS/ Dble(NATS)

! ................................................
!    CONSTRUCCION DEL SISTEMA INICIAL
! ................................................
      If ( INIC == 0 ) Then
         nprom = 0
         Call REDES 
         Call SETV0 ( TEMP )
         igaa(:) = 0.d0
         igab(:) = 0.d0
         if ( Lparedx ) then
           igw1(:) = 0.d0
           igw2(:) = 0.d0
           igw3(:) = 0.d0
         end if
         if ( Lparedy ) then
           igwy(:) = 0.d0
         end if
      Else
         If ( INIC == 3 ) Then
            VX(1:NATS) = 0.00d0
            VY(1:NATS) = 0.00d0
         End If
      End If
! ######  Modificacion del procedimiento de asignacion de etiquetas
! ######  y asignacion de masas relativas
      Do j=1, ncomp
         Do i=natcm(j-1)+1,natcm(j)
            lab(i) = j
            If (Abs(fgy).Gt.1.0d-4) Then
               If (lab(i) == 1) Amass(i) = 1.0d0
               If (lab(i) == 2) Amass(i) = 4.0d0
               If (lab(i) == 3) Amass(i) = 9.0d0
            Else
               Amass(i) = 1.0d0
            Endif
!            Write(888,*)i,lab(i)
         End Do
      End Do
      Do ic = 1, ncb
         Call ENERGY (ncomp)
         Call MOVES
         Call KINETE ( TATOM )

         if ( Lpared ) then
           presio = Ppared
         else
           PRESIO = PIDEAL * TATOM + VLJ / VBOX
         end if
         ETOTAL = ELJ+ EKINET + upared + ugrav
         SELJ = SELJ + ELJ
         SKINET = SKINET + EKINET
         SETOTA = SETOTA + ETOTAL
         SPRES = SPRES + PRESIO
         STATOM = STATOM + TATOM
         if ( Lpared ) then
           supar = supar + upared
           if ( Lparedy ) then
             sugrav = sugrav + ugrav
           end if
         end if

!  * * *   CALCULO DE PROPIEDADES * * *
!  * * *   acualizacion de coordenadas
!  * * *   Escalado de velocidades

         Call CHANGE 

         If ( SCAFLG > 0.0d0 ) Then
            If ( ic == (ic/FRSCAL) * FRSCAL ) Then
               Call ESCALE ( TEMP,SCAFLG )
            End If
         End If
      End Do

!  * * *   CALCULO DE promedios ***

      R  = 1.00d0 / Dble(NATS)/Dble(ncb)
      RR = R / Dble(NATS)
     
      ELJ   = SELJ   * R
      ENKT  = SKINET * R
      ETOT  = SETOTA * R
     

      PRES  = SPRES  / Dble(ncb)
      SPRESM = SPRESM + PRES
      TEMAT = STATOM / Dble(ncb)
       if ( Lpared ) then
         upar = supar/dble(nats)/dble(ncb)
         if ( Lparedy ) then
           ugrav = sugrav/dble(nats)/dble(ncb)
!           write(12,1201) etot, elj, upar, ugrav, pres, temat
!         else
!           write(12,1201) etot, elj, upar, pres, temat
         end if
!       else
!         write(12,1201) etot, elj,  pres, temat
       end if
 1201  format(1x,6(f13.6,1x) )
      
      Call SET0
      if ( lpared ) then
 
!     Se calcula el perfil de densidad
!     Intervalo (x=-sigma,box+sigma)
!     El perfil debe ser practicamente simetrico si la fase
!     fluida es homogenea.
 
        if ( Lparedx ) then
           dr = (xppared-xmpared)/Dble(nhp)
           Do i=1,nats
            ic = int( (x(i)-xmpared)/dr )
            if ( lab(i).eq.1 ) then
              igw1(ic) = igw1(ic) + 1.d0
            else if ( lab(i).eq.2 ) then
              igw2(ic) = igw2(ic) + 1.d0
            else if ( lab(i).eq.3 ) then
              igw3(ic) = igw3(ic) + 1.d0
            end if
          end do
        end if
        if ( Lparedy ) then
           dr = (yppared-ympared)/Dble(nhp)
           Do i=1,nats
            ic = int( (y(i)-ympared)/dr )
            igwy(ic) = igwy(ic) + 1.d0
          end do
        end if
      end if
! 
      dr = dri/BOX
      Do i=1,nimp-1
       xi = X(i)
       yi = Y(i)
       Do j = i+1, nimp-1
            dy = yi - Y(j) 
            dx = xi - X(j)
            If ( .not. Lparedx ) then
              dx = dx - Nint(dx)
            end if
            If ( .not. Lparedy ) then
              dy = dy - Nint(dy)
            end if
            rr = Sqrt(dx**2 + dy**2)
            If (rr < 0.500d0) Then
               ic = Nint(rr/dr)
               If (ic <= ngm ) Then
                  If( lab(i) == lab(j) ) Then
                     igaa(ic) = igaa(ic) + 1.d0
                  Else
                     igab(ic) = igab(ic) + 1.d0
                  End If
               End If
            End If
         End Do
      Enddo
! 
      do i=1,Nats
        if ( Lparedx ) then
          xxx(i) = X(i) - 0.5d0
        else
          xxx(i) = X(i) - Nint(X(i))
        end if
        if ( Lparedy ) then
          yyy(i) = Y(i) - 0.5d0
        else
          yyy(i) = Y(i) - Nint(Y(i))
        end if 
        iax(i) = (xxx(i) + 0.50)*isize + 0.50 + (irsize-isize)/2
        iay(i) = (yyy(i) + 0.50)*isize + 0.50 + (irsize-isize)/2
      End do

      nprom = nprom + 1
      ndmax = Min(Nint(0.500d0/dr),ngm)

      cte = 0.50*(dens*Dble(nprom*NATS))
      Do i = 1, ndmax
         ds = PI*(((i + 0.5)*dr)**2-((i - 0.5)*dr)**2)*box**2
         rnideal = ds*cte
         graa(i) = igaa(i)/rnideal
         If (ncomp > 1) Then
            graa(i) = ncomp*graa(i)
            grab(i) = igab(i)/rnideal
            grab(i) = ncomp*grab(i)/Dble(ncomp-1)
         End If
      End Do

      graa(0) = Maxval(graa(1:ndmax))

      If(ncomp > 1) Then
         grab(0) = Maxval(grab(1:ndmax))
         graa(0) = Max(graa(0),grab(0))
      Endif
!
      if ( Lpared ) then
        dr = (xppared-xmpared)/dble(nhp)
        vc = dr*box**2
        cn = 1.d0/dble(nprom)/vc
        If ( Lparedx ) Then
          gw1(:) = cn*igw1(:)
          gw2(:) = cn*igw2(:)
          gw3(:) = cn*igw3(:)
          gw1(0) = Maxval(gw1(1:ngmx))
          gw2(0) = Maxval(gw2(1:ngmx))
          gw1(0) = Max(gw1(0),gw2(0))
       End If
        If ( Lparedy ) Then
           dr = (yppared-ympared)/Dble(nhp)
           vc = dr*box**2
           cn = 1.d0/Dble(nprom)/vc
           gwy(:) = cn*igwy(:)
           gwy(0) = Maxval(gwy(1:ngmx))
        End If
      end if
!!      pres = spresm/Dble(nprom)
      nc_old = ncomp
      ra_old = rci
      rb_old = rcd
      tr_old = temp
      Inicia = .False.
!
!

      Return
  2101 format(2(1x,f15.6))
    End Subroutine mdmezcla


    Subroutine REDES 
      Use Datos
      Use Coordenadas

! .........................
!   ---> Red bidimensional
! .........................

      Implicit None

      Integer (kind=4) :: nc , na , i , ic , ix , iy , ipc
      Integer (kind=4) :: mx, my, mxo, myo
      Real (kind=8) :: dx, dy, d11, dv, dvmx

      nc = (1+NATS)/2
      mxo = 0
      myo = 0
      dvmx = 0.d0
!
      mx = 0
      my = 0
      Do While ( my <= mx )
         my = my + 1
         mx = 1 + int ( dble(nc)/dble(my) - 0.0001d0 )
         if ( mx.ge.my ) then
           dx = 1.d0/dble(mx) 
           dy = 1.d0/dble(my) 
           d11 = 0.5d0*sqrt(dx**2+dy**2)
           dv = dmin1(dx,d11)
           if ( dv.gt.dvmx ) then
             dvmx = dv
             mxo = mx
             myo = my
           end if
         end if
      End Do
! Choose random positions for each atom species      
      Call shuffle(New,NATS)
      mx = mxo
      my = myo
      dx = 1.d0/dble(mx)
      dy = 1.d0/dble(my)
      Do i=1, NATS
        ic = (i-1)/2
        ix = mod(ic,mx)
        iy = ic/mx
        ipc = Mod(i-1,2)
        X(New(i)) = ix*dx + ipc*(dx/2.d0)
        Y(New(i)) = iy*dy + ipc*(dy/2.d0)
!        X(i) = ix*dx + ipc*(dx/2.d0)
!        Y(i) = iy*dy + ipc*(dy/2.d0)
!!        write(11,1101) x(i), y(i)
      End Do
!!  1101  format(2(1x,f12.5))

      Return
    End Subroutine REDES

Subroutine shuffle(New,n)
  Implicit None
  Integer, Dimension(n) :: New
  Integer, Intent(in) :: n
  Integer  ::  count, ipos,i
  Real(kind=8) :: rnd
  logical :: busy(2000)
  busy(:) = .false.
  Do count = 1, n
     Call Random_number(rnd)
     ipos = Int(rnd*n)+1
     Do While (busy(ipos))
        Call Random_number(rnd)
        ipos = int(rnd*n)+1
     End Do
     New(ipos) = count
     busy(ipos) = .True.
  End Do
End Subroutine shuffle


    Subroutine ENERGY (ncomp)
      Use Comun
      Use Fuerza
      Use Coordenadas
      Use Labels
      Use Datos
      Use Ener_vir
      Use Potencial
      Use Caja
      
!     E N E R G Y
!    CALCULATES ENERGY AND FORCES OVER THE SITES

      Implicit None

      Logical :: igual, loeq, lodi
      Integer (kind=4) :: lc(MATS), Tail(0:NCMX-1), Head(0:NCMX-1) , &
           &                   i , ic , ix , iy , it , ica , ia , icv , ib , icb
      Real (kind=8), Dimension (MATS) :: de13x , de13y , de7x , de7y
      Real (kind=8) :: UELJ12 , UELJ6 , UECaa , UECab , d7x , d7y , &
           &           d13x , d13y , RRIJ2 , RRIJ6 , RRIJ12 , &
           &           RRIJ14 , WX , WY , RIJSQ , RRIJ8
      Real (kind=8) :: apared, xim, xip, fxi, yim, yip, fyi
      Real (kind=8) ::  xiw, yiw, rxiw2, rxiw4, rxiw6, upi, rrcp, ucpared
      Real (kind=8) ::  FWL, FWR, FWU, FWD
      Integer (kind=4) :: Nip(3), li, ncomp, Nipy

      ELJ    = 0.00d0
      VLJ    = 0.00d0
      UELJ12 = 0.00d0
      UELJ6  = 0.00d0
      UECaa  = 0.00d0
      UECab  = 0.00d0
      Upared = 0.d0
      Ugrav = 0.d0

      if ( Lparedx .or. Lparedy ) then
        apared = 5.d0*(5.d0/2.d0)**(2.d0/3.d0)/3.d0
        apared = apared*epared
        if ( Lparedx ) then
          do i=1,ncomp
            Nip(i) = 0
          end do
!    Fuerzas que actuan sobre paredes izqda (L) y dcha(R)
          FWL = 0.d0
          FWR = 0.d0
        end if
        if ( Lparedy ) then
          Nipy = 0
!    Fuerzas que actuan sobre paredes superior (U) e inferior (D)
          FWU = 0.d0
          FWD = 0.d0
        end if
      end if
!
!
      de13x (1:NATS)  = 0.00d0
      de13y (1:NATS)  = 0.00d0
      de7x  (1:NATS)  = 0.00d0
      de7y  (1:NATS)  = 0.00d0

!   --> Link-cells

      Head(0:nl2-1) = 0
      Tail(0:nl2-1) = 0

      Do i=1,NATS
! 
         if ( Lparedx ) then
           ix = Int (nl*X(i))
           ix = max0 ( ix, 0 )
           ix = min0 ( ix, nl-1)
           if ( X(i) < xmpared .or. X(i) > xppared ) then
             write(*,*) ' PRECAUCION: (box) ', box
             write(*,*) ' i, X(i) ', i, box*X(i)
             stop
           end if 
         else
           X(i) = X(i) - Nint(X(i) - 0.50d0)
           ix = Int (nl*X(i))
           ix = Mod(ix,nl)
           If ( ix < 0 .Or. ix >= nl ) Then
              write(*,*) ' X(i) = ', X(i)
              Write(*,*) ' ix = ', ix,nl
              stop
           End If
         end if
!
         if ( Lparedy ) then
           iy = Int (nl*Y(i))
           iy = max0 ( iy, 0 )
           iy = min0 ( iy, nl-1)
           if ( Y(i) < ympared .or. Y(i) > yppared ) then
             write(*,*) ' PRECAUCION: (box) ', box
             write(*,*) ' i, Y(i) ', i, box*Y(i)
             stop
           end if 
         else 
           Y(i) = Y(i) - Nint(Y(i) - 0.50d0)
           iy = Int (nl*Y(i))
           iy = Mod(iy,nl)

           If ( iy < 0 .Or. iy >= nl ) Then
             write(*,*) ' Y(i) = ', Y(i)
             Write(*,*) ' iy = ', iy,nl
             stop
           End If
         end if

         ic = ix*nl + iy

         If ( Head(ic) == 0 ) Then
            Head(ic) = i
            Tail(ic) = i
            lc(i) = 0
         Else
            it = Tail(ic)
            lc(it) = i
            Tail(ic) = i
            lc(i) = 0
         End If
! 
!  INTERACCION CON LAS PAREDES LATERALES
!
         If ( Lparedx ) Then
           li = lab(i)
           FX(i) = 0.d0
           xim = X(i)-xmpared
           xim = box*(X(i)-xmpared)
           xiw = abs(xim)
           if ( xiw < rcparedx(li) ) then
             rxiw2 = 1.d0/xiw**2
             rxiw4 = rxiw2*rxiw2
             rxiw6 = rxiw4*rxiw2
             upi = apared*(rxiw4*(rxiw6-1.d0))
             Nip(li) = Nip(li) + 1 
             Upared = Upared + upi
             fxi = apared*(rxiw6*(10*rxiw6-4.d0))*xim
             Fx(i) = fxi*box
             FWL = FWL + fxi
           end if
!
           xip = box*(X(i)-xppared)
           xiw = abs(xip)
           if ( xiw < rcparedx(li) ) then
             rxiw2 = 1.d0/xiw**2
             rxiw4 = rxiw2*rxiw2
             rxiw6 = rxiw4*rxiw2
             upi = apared*(rxiw4*(rxiw6-1.d0))
             Nip(li) = Nip(li) + 1 
             Upared = Upared + upi
             fxi = apared*(rxiw6*(10*rxiw6-4.d0))*xip
             Fx(i) = Fx(i) + fxi*box
             FWR = FWR - fxi
           end if
        End If

! 
!  INTERACCION CON LAS PAREDES SUPERIOR E INFERIOR
!        + CAMPO GRAVITATORIO
!
        If ( Lparedy ) Then
           li = lab(i)
           FY(i) = -fgyb
           Ugrav = Ugrav + fgyb*Y(i)
           Yim = Y(i)-ympared
           yim = box*(Y(i)-ympared)
           yiw = abs(yim)
           If ( yiw < rcparedy ) Then
             rxiw2 = 1.d0/yiw**2
             rxiw4 = rxiw2*rxiw2
             rxiw6 = rxiw4*rxiw2
             upi = apared*(rxiw4*(rxiw6-1.d0))
             Nipy    = Nipy    + 1 
             Upared = Upared + upi
             fyi = apared*(rxiw6*(10*rxiw6-4.d0))*yim
             Fy(i) = fyi*box
             FWD = FWD + fyi
          End If
!
          yip = box*(Y(i)-yppared)
          yiw = Abs(yip)
          If ( yiw < rcparedy ) Then
             rxiw2 = 1.d0/yiw**2
             rxiw4 = rxiw2*rxiw2
             rxiw6 = rxiw4*rxiw2
             upi = apared*(rxiw4*(rxiw6-1.d0))
             Nipy = Nipy + 1 
             Upared = Upared + upi
             fyi = apared*(rxiw6*(10*rxiw6-4.d0))*yip
             Fy(i) = Fy(i) + fyi*box
             FWU = FWU - fyi
          End If
          !
       End If
       !
    End Do
    If ( Lparedx  ) Then
       Do i=1,ncomp
          rrcp = 1.d0/rcparedx(i)
          ucpared = apared*(rrcp**4*(rrcp**6-1.d0))
          Upared = Upared - Nip(i)*ucpared
       End Do
    End If
    If ( Lparedy  ) Then
       rrcp = 1.d0/rcparedy
       ucpared = apared*(rrcp**4*(rrcp**6-1.d0))
       Upared = Upared - Nipy*ucpared
    End If
 
    If ( Lparedx ) Then
       If ( Lparedy ) Then
          Ppared = (FWL+FWR+FWU+FWD)/(4*box)
       Else 
          Ppared = (FWL+FWR)/(2*box)
       End If
    Else If ( Lparedy ) Then
       Ppared = (FWU+FWD)/(2*box)
    End If

    Do ica = 0, nl2-1
       ia = head(ica)
       Do While ( ia.Ne.0 ) 
! -------------NUMERO DE CELDAS VECINAS VARIABLE ---------
            Do icv = 1, ncv(ica)
               icb = lcv(icv,ica)
               ib = head(icb)
               Do While ( ib.Ne.0 ) 
                  WX = X ( IA ) - X ( IB )
                  WY = y ( IA ) - y ( IB )
!
                  if ( .not. Lparedx ) then
                    WX = WX - Nint ( WX )
                  end if
!
                  if ( .not. Lparedy ) then
                    WY = WY - Nint ( WY )
                  end if
                  RIJSQ = WX * WX + WY * WY
                  
                  igual = lab(ia).Eq.lab(ib)
                  loeq =  igual .And. RIJSQ .Le. RCaa2 
                  lodi =  (.Not.igual).And. RIJSQ .Le. RCab2 
                  If ( loeq .Or. lodi ) Then

                     RRIJ2  =   1.00D0 / RIJSQ
                     RRIJ6  = RRIJ2  * RRIJ2 * RRIJ2
                     RRIJ8  = RRIJ6  * RRIJ2
                     RRIJ12 = RRIJ6  * RRIJ6
                     RRIJ14 = RRIJ12 * RRIJ2

                     If ( loeq ) Then
                        uecaa = uecaa +  1.00d0
                     Else
                        uecab = uecab +  1.00d0
                     End If

                     UELJ6  = UELJ6 + RRIJ6
                     UELJ12 = UELJ12 + RRIJ12

                     D7X  = WX * RRIJ8
                     D13X = WX * RRIJ14
                     D7Y  = WY * RRIJ8
                     D13Y = WY * RRIJ14

                     de7x (ia) = de7x(ia) + d7x
                     de7y (ia) = de7y(ia) + d7y
                     de7x (ib) = de7x(ib) - d7x
                     de7y (ib) = de7y(ib) - d7y

                     de13x (ia) = de13x(ia) + d13x
                     de13y (ia) = de13y(ia) + d13y
                     de13x (ib) = de13x(ib) - d13x
                     de13y (ib) = de13y(ib) - d13y

                  End If
                  ib = lc(ib)
               End Do
            End Do
            ia = lc(ia)
         End Do

         ia = head(ica)
         Do While ( ia /= 0 ) 
            ib = lc(ia)
            Do While ( ib /= 0 ) 
               WX = X ( IA ) - X ( IB )
               WY = y ( IA ) - y ( IB )
!
               if ( .not. Lparedx ) then
                 WX = WX - Nint ( WX )
               end if
!
               if ( .not. Lparedy ) then
                 WY = WY - Nint ( WY )
               End if
               RIJSQ = WX * WX + WY * WY

               igual = lab(ia) == lab(ib)
               loeq =  igual .And. RIJSQ  <= RCaa2 
               lodi =  (.Not.igual).And. RIJSQ <= RCab2 
               If ( loeq .Or. lodi ) Then
                  RRIJ2  =   1.00D0 / RIJSQ
                  RRIJ6  = RRIJ2  * RRIJ2 * RRIJ2
                  RRIJ8  = RRIJ6  * RRIJ2
                  RRIJ12 = RRIJ6  * RRIJ6
                  RRIJ14 = RRIJ12 * RRIJ2

                  If ( loeq ) Then
                     uecaa = uecaa +  1.00d0
                  Else
                     uecab = uecab +  1.00d0
                  End If

                  UELJ6 = UELJ6 + RRIJ6
                  UELJ12 = UELJ12 + RRIJ12

                  D7X  = WX * RRIJ8
                  D13X = WX * RRIJ14
                  D7Y  = WY * RRIJ8
                  D13Y = WY * RRIJ14

                  de7x (ia) = de7x(ia) + d7x
                  de7y (ia) = de7y(ia) + d7y
                  de7x (ib) = de7x(ib) - d7x
                  de7y (ib) = de7y(ib) - d7y

                  de13x (ia) = de13x(ia) + d13x
                  de13y (ia) = de13y(ia) + d13y
                  de13x (ib) = de13x(ib) - d13x
                  de13y (ib) = de13y(ib) - d13y

               End If
               ib = lc(ib)
            End Do
            ia = lc(ia)
         End Do
      End Do

      ELJ = 4 * RBOX12 * UELJ12 - 4 * RBOX6 * UELJ6 
      VLJ = 24* RBOX12 * UELJ12 -12 * RBOX6 * UELJ6
      ELJ = ELJ - 4*UECaa*(RBOX12/RCaa12 - RBOX6/RCaa6 ) - &
           &             4*UECab*(RBOX12/RCab12 - RBOX6/RCab6 )

      if ( Lparedx ) then
        FX(1:NATS) = FX(1:NATS)+ 48*RBOX12*De13x(1:NATS)-24*RBOX6*DE7x(1:NATS)  
      else
        FX(1:NATS) = 48*RBOX12*De13x(1:NATS) - 24*RBOX6*DE7x(1:NATS)  
      end if
      if ( Lparedy ) then
        FY(1:NATS) = FY(1:NATS)+ 48*RBOX12*De13y(1:NATS)-24*RBOX6*DE7y(1:NATS)  
      else  
        FY(1:NATS) = 48*RBOX12*De13y(1:NATS) - 24*RBOX6*DE7y(1:NATS)  
      end if
     
      Return
    End Subroutine ENERGY


    Subroutine MOVES
      Use Coordenadas
      Use Datos , Only : NATS , TISTEP
      Use Fuerza, Only : Fx , Fy

! ......................................................
!     M O V E S
!     INTEGRATION OF EQUATIONS OF MOTION ( LEAP-FROG )
! ......................................................

      Implicit None

      !     CALCULATION OF VELOCITIES :  T + DT/2
      !     CALCULATION OF POSITIONS  :  T + DT

      VX1(1:NATS) = VX (1:NATS) + TISTEP * FX(1:NATS)
      VY1(1:NATS) = VY (1:NATS) + TISTEP * FY(1:NATS)
      X1(1:NATS)  = X(1:NATS)   + TISTEP * VX1(1:NATS)
      Y1(1:NATS)  = Y(1:NATS)   + TISTEP * VY1(1:NATS)

      Return
    End Subroutine MOVES


    Subroutine CHANGE
      Use Comun
      Use Coordenadas
      Use Datos

! ........................................................
!     C H A N G E : SET NEW POSITIONS AND VELOCITIES
! ........................................................

      Implicit None

      X(1:NATS)  = X1(1:NATS)
      Y(1:NATS)  = Y1(1:NATS)
      VX(1:NATS) = VX1(1:NATS)
      VY(1:NATS) = VY1(1:NATS)

      Return
    End Subroutine CHANGE


    Subroutine SETV0 (TEMP )
      Use Datos , Only : NATS
      Use Coordenadas, Only : VX , VY
      Use Aleatoria

!  ....................................................
!     S E T V 0
!     FIJA VELOCIDADES INICIALES, CON EL CRITERIO:
!      < VIX >= T* ** 1/2
!  ....................................................

      Implicit None
     
      Integer (kind=4) :: i
      Real (kind=8) :: VMAX , TEMP , RAN , SVX , SVY , VXM , VYM

      !  ############# Analizar factor

      VMAX = Sqrt ( 3.00d0 * TEMP )

      Do i = 1, NATS
         SEM =  Mod  ( 16807.00d0 * SEM, 2147483647.00d0 )
         RAN = SEM / 2147483648.00d0
         VX ( i ) = VMAX * ( 2.00d0 * RAN - 1.00d0 )
         SEM =  Mod  ( 16807.00d0 * SEM, 2147483647.00d0 )
         RAN = SEM / 2147483648.00d0
         VY ( i ) = VMAX * ( 2.00d0 * RAN - 1.00d0 )
      End Do

!     MOVIMIENTO GLOBAL, CALCULO Y ELIMINACION

      SVX = Sum(VX(1:NATS))
      SVY = Sum(VY(1:NATS))

      VXM = SVX / Dble ( NATS )
      VYM = SVY / Dble ( NATS )

      VX(1:NATS) = VX(1:NATS) - VXM
      VY(1:NATS) = VY(1:NATS) - VYM
     
      Return
    End Subroutine SETV0


    Subroutine ESCALE ( TEMP , SCAFLG )
      Use Comun
      Use Coordenadas, Only : VX , VY, Amass
      Use Datos

! ..........................................................
!     E S C A L E
!     RE-ESCALING VELOCITIES ACCORDING TO THE TEMPERATURE
! ..........................................................

      Implicit None
     
      Real (kind=8) :: SV2 , ESC , TEMP , SCAFLG , TRUN

!      SV2 = Sum(VX(1:NATS)**2)  + Sum(VY(1:NATS)**2)
      SV2 = Sum(Amass(1:NATS)*VX(1:NATS)**2)  + Sum(Amass(1:NATS)*VY(1:NATS)**2)

      TRUN = SV2 / Dble( 2*NATS-2)
      ESC = 1.0d0 + SCAFLG * (Sqrt ( TEMP / TRUN ) - 1.0d0)

      VX(1:NATS) = ESC*VX(1:NATS)
      VY(1:NATS) = ESC*VY(1:NATS)

      Return
    End Subroutine ESCALE


    Subroutine KINETE ( TATOM )
      Use Comun
      Use Ener_vir
      Use Coordenadas
      Use Datos

!  ...............................................
!     K I N E T E
!     CALCULATION OF VELOCITIES AND 'TEMPERATURES'
!  ...............................................

      Implicit None
      
      Real (kind=8) :: TATOM

      VX(1:NATS) = 0.500d0*(VX(1:NATS) + VX1(1:NATS))
      VY(1:NATS) = 0.500d0*(VY(1:NATS) + VY1(1:NATS))
!      EKINET = Sum(VX(1:NATS)*VX(1:NATS) + VY(1:NATS)*VY(1:NATS))
      EKINET = Sum(Amass(1:NATS)*VX(1:NATS)*VX(1:NATS) + Amass(1:NATS)*VY(1:NATS)*VY(1:NATS))

      EKINET = 0.500D0 * EKINET
      TATOM = EKINET / Dble(NATS-1)

      Return
    End Subroutine KINETE


    Subroutine SET0
      Use Sumas

! ...............................................................
!     S E T 0
!     MAKES 0 ALL THE SUMS OF ENERGY, VIRIAL AND TEMPERATURES
! ...............................................................

      Implicit None

      SELJ   = 0.00d0
      SKINET = 0.00d0
      SETOTA = 0.00d0
      SPRES  = 0.00d0
      STATOM = 0.00d0
      supar = 0.d0
      sugrav = 0.d0

      Return
    End Subroutine SET0


    Subroutine Liscv (Lparedx,Lparedy) 
      Use Comun
      Use Labels

      Implicit None

      Integer (kind=4) :: i , ix , iy , ixm , ixp , iym, ncvi
      Logical :: Lparedx, Lparedy
      Logical :: LMM, L0M, LPM, LM0

      nl =  Min ( NLMX, Max( nl, 3 ) )
      nl2 = nl*nl

      Do i=0,nl2-1
         ix = i/nl
         iy = Mod(i,nl)
         LMM = .true.
         L0M = .true.
         LPM = .true.
         LM0 = .true.
         if ( Lparedx ) then
           LMM = ix > 0
           LM0 = ix > 0
           LPM = ix < nl-1
         end if
         if ( Lparedy ) then
           LMM = LMM .and. iy > 0
           L0M = iy > 0
           LPM = LPM .and. iy > 0
         end if
         ixm = Mod ( nl+ix-1, nl)
         ixp = Mod (    ix+1, nl)
         iym = Mod ( nl+iy-1, nl)
 
         ncvi = 0
          
         if ( LMM  ) then
           ncvi = ncvi+1 
           lcv(ncvi,i) = ixm*nl+iym
         end if
         if ( L0M ) then
           ncvi = ncvi+1 
           lcv(ncvi,i) = ix *nl+iym
         end if
         if ( LPM ) then
           ncvi = ncvi+1 
           lcv(ncvi,i) = ixp*nl+iym
         end if
         if ( LM0 ) then
           ncvi = ncvi+1 
           lcv(ncvi,i) = ixm*nl+iy
         end if
         ncv(i) = ncvi

      End Do
      
      Return
    End Subroutine Liscv

