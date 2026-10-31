#include <stdio.h>
#include <jni.h>
#include "Simulation.h"

JNIEXPORT void JNICALL Java_Simulation_jsimul
  (JNIEnv *env, jobject obj, jintArray arr, jintArray arr1, jdoubleArray arr2, jdoubleArray arr3, jdoubleArray graa, jdoubleArray grab, jdouble drr, jdouble tst, jdouble rci, jdouble rcd, jdouble temp, jdouble dens, jdouble scal, jint ifr , jint ncomp , jint len, jint ngr, jint ngry, jint isize, jint irsize , jint inc , jint ncp1, jdoubleArray gw1, jdoubleArray gw2,jdoubleArray gw3, jdoubleArray gw4, jdouble rcw1, jdouble rcw2, jdouble rcw3, jdouble rwyy, jdouble epsw, jint ipared, jint iparedy, jdouble fgy, jintArray natcm, jint idum, jdouble elon, jint ihet )
{
#ifdef WIN32
    void MDMEZCLA(int[] , int[], double[], double[], double[], double[], double*, double*, double*, double*, double*, double*, double*, double*, int*, int*, int*, int*, int*, int*, int*, int* , int*, double[], double[], double[], double[], double*, double*, double*, double*, double*, int*, int*, double*, int[], int*, double*, int* );
#else
    void mdmezcla_(int[] , int[], double[], double[], double[], double[], double*, double*, double*, double*, double*, double*, double*, double*, int*, int*, int*, int*, int*, int*, int*, int* , int*, double[], double[], double[], double[], double*, double*, double*, double*, double*, int*, int*, double*, int[], int*, double*, int* );
#endif
    jint *carr;
    jfieldID fid;
    jint kkk[6001], kk2[6001], natcm1[10];
    jdouble xxx[6001], yyy[6001], gga[6001] , ggb[6001], gwa[6001], gwb[6001], gwc[6001], gwy[6001];
    jint i;
    int ilen, iss, inic, ifrsc, ncp, ncmp, iwall, iwally, idumb, iheter ;
    double tistep, rcaa, rcab, temper, densty , scalf, rwa, rwb, rwc, rwy,eps, pres0, fgy0,elong;
    jclass cls = (*env)->GetObjectClass(env, obj);
    fid = (*env)->GetStaticFieldID(env,cls,"pres","D");
    ilen = len;
    iss = isize;
    inic = inc;
    scalf = scal;
    ifrsc = ifr;
    iwall = ipared;
    iwally = iparedy;
    fgy0 = fgy;
    rwa = rcw1;
    rwb = rcw2;
    rwc = rcw3;
    eps = epsw;
    rwy = rwyy;
    ncp = ncp1;
    tistep = tst;
    rcaa = rci;
    rcab = rcd;
    temper = temp;
    densty = dens;
    ncmp = ncomp;
    elong = elon;
    idumb = idum;
    iheter = ihet;
    (*env)->GetIntArrayRegion(env, natcm, 0, ncmp+1, natcm1 );
    /*    printf(" natm in  %d %d %d %d\n ",natcm1);*/
/*  carr = (*env)->GetIntArrayElements(env, arr, NULL); */
#ifdef WIN32
    MDMEZCLA(kkk, kk2, xxx, yyy, gga, ggb, &pres0, &drr, &tistep, &rcaa, &rcab, &temper, &dens, &scalf, &ifrsc, &ncmp , &ilen, &ngr, &ngry, &isize, &irsize, &inic, &ncp, gwa, gwb, gwc, gwy, &rwa, &rwb, &rwc, &rwy, &eps, &iwall, &iwally, &fgy0, natcm1, &idumb, &elong, &iheter ); 
#else
    mdmezcla_(kkk, kk2, xxx, yyy, gga, ggb, &pres0, &drr, &tistep, &rcaa, &rcab, &temper, &dens, &scalf, &ifrsc, &ncmp , &ilen, &ngr, &ngry, &isize, &irsize, &inic, &ncp, gwa, gwb, gwc, gwy, &rwa, &rwb, &rwc, &rwy, &eps, &iwall, &iwally, &fgy0, natcm1, &idumb, &elong, &iheter ); 
#endif
 (*env)->SetIntArrayRegion(env, arr, 0, len, kkk );
 (*env)->SetIntArrayRegion(env, arr1, 0, len, kk2 );
 (*env)->SetDoubleArrayRegion(env, arr2, 0, len, xxx );
 (*env)->SetDoubleArrayRegion(env, arr3, 0, len, yyy );
 (*env)->SetDoubleArrayRegion(env, graa, 0, ngr, gga );
 (*env)->SetDoubleArrayRegion(env, grab, 0, ngr, ggb );
 (*env)->SetDoubleArrayRegion(env, gw1, 0, ngr, gwa );
 (*env)->SetDoubleArrayRegion(env, gw2, 0, ngr, gwb );
 (*env)->SetDoubleArrayRegion(env, gw3, 0, ngr, gwc );
 (*env)->SetDoubleArrayRegion(env, gw4, 0, ngry, gwy );
 (*env)->SetStaticDoubleField(env,cls,fid,pres0);
/*  (*env)->ReleaseIntArrayElements(env, arr, carr,0); */
 return ;
 }

  
