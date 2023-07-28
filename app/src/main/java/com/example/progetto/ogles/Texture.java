package com.example.progetto.ogles;

import static android.opengl.GLES11Ext.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_NEAREST;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_REPEAT;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetFloatv;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glTexParameteri;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Classe per la gestione di una generica texture.
 *
 * Tiene il riferiemnto all' ID (handle) del TextureObject.
 *
 * Viene utile nel caso in cui diverse istanze di MaterialBasic (con eventuali valori diversi
 * di uniform es. scaling) utilizzano la stessa Texture.
 */
public class Texture {

    private static String TAG;

    private final int[] textureObjId;  // handle
    private final Bitmap bitmap;

    /**
     * Costruttore della classe.
     *
     * Crea il texture object sulla base del bitmap specificato.
     *
     * @param bitmap Btimap da usare come texture
     */
    public Texture(Bitmap bitmap, boolean anisFilter){

        TAG = getClass().getSimpleName();

        textureObjId = new int[1];
        this.bitmap = bitmap;

        glPrepare(anisFilter);

    }

    /**
     * Metodo statico per il caricamento di un bitmap da file.
     *
     * @param context Activity context
     * @param idDrawable ID risorsa
     * @return Immagine Bitmap caricata
     */
    public static Bitmap loadBitmap(Context context, int idDrawable){

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled=false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), idDrawable, opts);

        if(bitmap!=null)
            Log.d(TAG,"bitmap of size " + bitmap.getWidth()+"x"+bitmap.getHeight()+ " loaded " +
                    "with format " + bitmap.getConfig().name());

        return bitmap;

    }

    /**
     * Funzione che crea Texture Object ed imposta i parametri della texture.
     */
    private void glPrepare(boolean anisFilter){

        glGenTextures(1, textureObjId, 0);

        glBindTexture(GL_TEXTURE_2D, textureObjId[0]);

            // quando texture la vedo da lontano (piccola) --> GL_LINEAR_MIPMAP_NEAREST
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
            // quando texture la vedo da vicino (grande) --> GL_LINEAR
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            // trasferimento sul device
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            glGenerateMipmap(GL_TEXTURE_2D);

            if(anisFilter) {
                // valore che rappresenta quanto è aggressivo il filtro. Più è alto
                // più è costosa la renderizzazione
                float[] maxAF = new float[1];
                glGetFloatv(GLES11Ext.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, maxAF, 0);
                int error =glGetError();
                if(error != GL_NO_ERROR) Log.d(TAG,"Error " + error);
                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAF[0]);
                if(error!= GL_NO_ERROR) Log.d(TAG,"Error " + error);
                Log.d(TAG, "Setted Anisotropic filtering (" + maxAF[0] +")");  // 16
            }

        glBindTexture(GL_TEXTURE_2D,0);

    }

    /****** GETTER *******/

    public int[] getTextureObjId() { return textureObjId; }  // handle
}
