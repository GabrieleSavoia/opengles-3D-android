package com.example.progetto.ogles.shader;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;

import android.util.Log;

import com.example.progetto.ogles.Texture;

/**
 * Classe che permette di assegnare un colore uniforme oppure una Texture (con relativo scaling
 * delle uv) ad un generico Object3D.
 *
 * Se non specificato nel costruttore, durante l'istanziamento della classe viene creato un nuovo
 * ShaderProgram a partire dai VSHADER e FSHADER di questa classe.
 *
 * IMPORTANTE:
 * Se invece al costruttore viene passato un ShaderProgram esistente, allora il MaterialBasic
 * creato punterà al ShaderProgram specificato (necessario quando si usano tanti MaterialBasic in
 * quanto condividono gli stessi VSHADER e FSHADER quindi non serve usare un nuovo program).
 */
public class MaterialBasic {

    private static String TAG;

    private final ShaderProgram program;
    private final Texture texture;

    private final float[] textureScaling;
    private float[] color;
    private int textured;

    public static final String VSHADER = "#version 300 es\n" +
            "\n" +
            "layout(location = 1) in vec3 vPos;\n" +
            "layout(location = 2) in vec2 vUV;\n" +
            "uniform mat4 MVP;\n"+
            "uniform vec2 texScaling;\n"+
            "out vec2 varyingvUV;\n"+
            "\n" +
            "void main(){\n" +
                "varyingvUV = vUV * texScaling;\n"+
                "gl_Position = MVP * vec4(vPos,1);\n" +
            "}";

    public static final String FSHADER = "#version 300 es\n" +
            "\n"+
            "precision mediump float;\n" +       // +- 2^14  fract. accuracy = 2^-10
            "uniform sampler2D tex;\n"+
            "uniform int textured;\n"+
            "uniform vec3 color;\n"+
            "in vec2 varyingvUV;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
                "if (textured == 1){"+
                    "fragColor = texture(tex, varyingvUV);\n"+
                "}else{"+
                    "fragColor.rgb = color;\n"+
                "}"+
            "}";

    public static final String[] UNIFORMS = new String[]{"MVP", "texScaling", "tex", "color", "textured"};

    /**
     * Costruttore della classe.
     *
     * Crea un nuovo ShaderProgram a partire dal Vertex e Fragment Shader della classe.
     *
     * Dopo aver creato il ShaderProgram, setta la uniform relativa al sampler della texture
     * nel fragment.
     *
     * @param texture Texture
     */
    public MaterialBasic(Texture texture){

        this(new ShaderProgram(VSHADER, FSHADER, UNIFORMS), texture, new float[]{1, 1});

        setTextureSamplerUniform();

    }

    /**
     * Costruttore della classe che punta ad un ShaderProgram esistente.
     *
     * Permette di impostare un colore uniforme (non è presente la texture).
     *
     * @param program ShaderProgram
     * @param color Colore [r, g, b]
     */
    public MaterialBasic(ShaderProgram program, float[] color){

        this(program, null, new float[]{1, 1});

        this.color = color;
        this.textured = 0;

    }

    /**
     * Costruttore della classe che punta ad un ShaderProgram esistente.
     *
     * Permette di impostare una Texture.
     * Le uv non sono scalate.
     *
     * @param program ShaderProgram
     * @param texture Texture
     */
    public MaterialBasic(ShaderProgram program, Texture texture){

        this(program, texture, new float[]{1, 1});

    }

    /**
     * Costruttore della classe che punta ad un ShaderProgram esistente.
     *
     * Permette di impostare una texture e lo scaling delle uv.
     *
     * @param program ShaderProgram
     * @param texture Texture
     * @param textureScaling Fattore di scaling delle uv ['scaleU', 'scaleV']
     */
    public MaterialBasic(ShaderProgram program, Texture texture,
                         float[] textureScaling){

        TAG = getClass().getSimpleName();

        this.program = program;
        this.texture = texture;

        this.textureScaling = textureScaling;
        this.color = new float[] {0, 0, 0};
        this.textured = 1;

    }

    /**
     * Il sampler "tex" si riferisce alla active texture GL_TEXTURE0.
     *
     * LINK: https://learnopengl.com/Getting-started/Textures
     *       https://stackoverflow.com/questions/54931941/correspondance-between-texture-units-and-sampler-uniforms-in-opengl
     */
    public void setTextureSamplerUniform(){

        Log.d(TAG, "setTextureSamplerUniform called");

        glUseProgram(program.getProgramId());
            glUniform1i(program.getUniformLoc("tex"), 0);
        glUseProgram(0);

    }

    /**
     * Aggiorno la uniform MVP.
     *
     * @param MVP Matrice MVP
     */
    public void updateMVP(float[] MVP){

        glUniformMatrix4fv(program.getUniformLoc("MVP"), 1, false, MVP, 0);

    }

    /**
     * Aggiorno i valori delle uniform.
     */
    public void updateUniforms(){

        glUniform2f(program.getUniformLoc("texScaling"), textureScaling[0], textureScaling[1]);
        glUniform1i(program.getUniformLoc("textured"), textured);
        glUniform3f(program.getUniformLoc("color"), color[0], color[1], color[2]);

    }

    /**
     * Attivo GL_TEXTURE0 e poi eseguo un bind tra la texture dello shader e GL_TEXTURE0
     *
     * LINK: https://learnopengl.com/Getting-started/Textures
     */
    public void activateTexture(){

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture.getTextureObjId()[0]);

    }

    public void setTextureScaling(float scaleX, float scaleY){

        textureScaling[0] = scaleX;
        textureScaling[1] = scaleY;

    }

    /****** GETTER *******/

    public ShaderProgram getShaderProgram() { return program; }

    public int getProgramId() { return program.getProgramId(); }

    public int getTextureID() { return texture.getTextureObjId()[0]; }

}
