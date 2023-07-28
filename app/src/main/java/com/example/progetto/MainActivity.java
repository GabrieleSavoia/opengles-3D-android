package com.example.progetto;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.Objects;

import com.example.progetto.game.GameRenderer;
import com.example.progetto.game.LabyrinthGame;

/**
 * Classe della Main Activity.
 *
 * LINK: Activity-lifecycle: https://developer.android.com/guide/components/activities/activity-lifecycle
 */
public class MainActivity extends AppCompatActivity{

    private GLSurfaceView surface;
    private boolean isSurfaceCreated;

    private LabyrinthGame game;

    /**
     * CREAZIONE activity.
     *
     * Ciclo completo di creazione:       onCreate() --> onStart() --> onResume()
     *
     * @param savedInstanceState Bundle
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d("TAG", "onCreate");

        // Full screen. Forzo lo screen orientation nel manifest
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();               // rimuove titolo

        // Guardo la versione OpenglES supportata dal dispositivo
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        int version = 1;
        if(configurationInfo.reqGlEsVersion>=0x30000)
            version = 3;
        else if(configurationInfo.reqGlEsVersion>=0x20000)
            version = 2;
        Log.d("TAG","OpenGLES supported >= " +
                version + " (" + Integer.toHexString(configurationInfo.reqGlEsVersion) + " " + configurationInfo.getGlEsVersion() + ")");

        // reqGlEsVersion = versione effettiva opengles dell'applicazione
        // getGlEsVersion () = estrae major and minor version di reqGLEsVersion e ritorna come stringa.

        // Creazione surfaceView
        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(version);
        surface.setPreserveEGLContextOnPause(true);     // con 'true', EGL context preservato quando
                                                        // GLSurfaceView è messo in pausa
                                                        // Es: quando app va in background EGL
                                                        // context non è distrutto --> NON si passa
                                                        // per 'onSurfaceCreated' del renderer


        game = new LabyrinthGame(this);

        // Creazione renderer
        GameRenderer renderer = new GameRenderer(game);
        setContentView(surface);
        renderer.setContextAndSurface(this, surface);
        surface.setRenderer(renderer);   // crea thread


        isSurfaceCreated = true;

    }

    /**
     * Chiamata dopo la onCreate().
     *
     * L'activity diventa VISIBILE all'utente (non ancora interattiva).
     *
     * Faccio ripartire il TransitionTimerTask: il thread eseguirà tutti i task in coda.
     *
     */
    @Override
    protected void onStart(){
        super.onStart();
        Log.d("TAG", "onStart");

        game.getTransitionTimerTask().wakeup();
    }

    /**
     * Chiamata dopo la onStart().
     *
     * L'activity diventa INTERATTIVA con l'utente.
     *
     * L'activity passa in stato RUNNING.
     *
     */
    @Override
    protected void onResume(){
        super.onResume();
        Log.d("TAG", "onResume");

        if(isSurfaceCreated)
            surface.onResume();
    }

    /**
     * L'activty NON INTERAGISCE più con l'utente.
     *
     * Quando premo HOME o OVERVIEW o BACK:                 onPause() --> onStop()
     *
     * Se riapro l'app (dalle recenti o dall'icona):        onStart() --> onResume()
     *
     * Se premo HOME o OVERVIEW o BACK e rientro, l'applicazione mantiene il suo stato.
     * Metto in sleep il TransitionTimerTask: il timer comunque continua a mettere in coda i task.
     */
    @Override
    protected void onPause(){
        super.onPause();
        Log.d("TAG", "onPause");

        if(isSurfaceCreated)
            surface.onPause();

        game.getTransitionTimerTask().sleep();
    }

    /**
     * L'activity NON è più VISIBILE.
     */
    @Override
    protected void onStop(){
        super.onStop();
        Log.d("TAG", "onStop");
    }

    /**
     * DISTRUZIONE dell'activity.
     *
     * Quando chiudo l'app:         onPause() --> onStop() --> onDestroy()
     *
     * Se riapro l'app:             onCreate() --> onStart() --> onResume()
     *
     */
    @Override
    protected void onDestroy(){
        Log.d("TAG", "onDestroy");
        game.getTransitionTimerTask().cancel();  // terminazione thread del timer task
        game.getTimer().cancel();                // termina il task cancellando i task schedulati
        game.getTimer().purge();                 // rimuove tutti i task dalla sua coda
        game.setTimer(null);
        game.setTransitionTimerTask(null);
        super.onDestroy();
    }

}