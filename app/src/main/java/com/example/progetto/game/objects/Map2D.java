package com.example.progetto.game.objects;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glScissor;
import static android.opengl.GLES20.glViewport;

import android.graphics.Point;
import android.opengl.GLES30;

import androidx.annotation.NonNull;

import java.util.Map;

import com.example.progetto.game.LabyrinthGenerator;

import com.example.progetto.ogles.Geometry3D;
import com.example.progetto.ogles.Object3D;
import com.example.progetto.ogles.camera.CameraOrtho2D;
import com.example.progetto.ogles.camera.CameraPersp3D;
import com.example.progetto.ogles.shader.MaterialBasic;

/**
 * Classe per la creazione della mappa 2D.
 *
 * E' sfruttata la funzionalità SCISSOR per posizionare la mappa in basso a destra e in sostanza
 * è come se con una camera ortogonale guardassi la scena (senza tetto) dall'alto.
 */
public class Map2D {

    private final LabyrinthGenerator labGenerator;

    private final CameraOrtho2D cameraOrtho;
    private final Point dimension;  // dimensione dello SCISSOR in pixel

    private final Object3D[] objLabyrinthWalls;
    private final Object3D objFloor;
    private final Object3D objStart;
    private final Object3D objEnd;

    /**
     * Costruttore della classe.
     *
     * Avviene la creazione della mappa 2D.
     *
     * Nella mappa ho un piano che rappresenta il pavimento (della stessa dimensione del pavimento
     * nel 3D), i muri del labirinto sono rappresentati da piani (di dimensione 1x1 e posizionati
     * nello stesso punto dei cubi nel 3D), infine le frecce sono rappresentate da triangoli.
     *
     * La mappa è visualizzata mediante camera ortogonale la cui dimensione (che sarà impostata
     * nella funzione 'setupProjection') è tale da inquadrare il labirinto più un bordo.
     *
     * @param labGenerator LabGenerator
     * @param geometries HashMap di geometrie
     * @param materials HashMap di materiali
     */
    public Map2D(LabyrinthGenerator labGenerator,
                 Map<String, Geometry3D> geometries,
                 Map<String, MaterialBasic> materials){

        cameraOrtho = new CameraOrtho2D();
        dimension = new Point();                   // dimensione SCISSOR in pixel

        this.labGenerator = labGenerator;
        Point dim = labGenerator.getDimension();   // dimensioni labirinto nello spazio

        // creazione piani che definiscono cubi labirinto
        float[][] labWalls = labGenerator.getWallsCoord();
        objLabyrinthWalls = new Object3D[labWalls.length];
        for (int i=0 ; i<labWalls.length ; i++){
            Object3D obj = new Object3D(geometries.get("plane"), materials.get("mapWall"));
            obj.setPosition(labWalls[i][0], -0.5f, labWalls[i][1]);
            obj.updateModelM();
            objLabyrinthWalls[i] = obj;
        }

        // Pavimento
        MaterialBasic floorMat = materials.get("mapFloor");
        assert floorMat != null;
        floorMat.setTextureScaling(dim.x, dim.y);
        objFloor = new Object3D(geometries.get("plane"), floorMat);
        objFloor.setPosition(0, -1f, 0);
        objFloor.setScale(dim.x, 1, dim.y);
        objFloor.updateModelM();

        // start (il position verrà settato successivamente)
        objStart = new Object3D(geometries.get("triangle"), materials.get("start"));
        objStart.setPosition(0f, 0f, 0f);
        objStart.updateModelM();

        // end
        objEnd = new Object3D(geometries.get("triangle"), materials.get("end"));
        float[] endPos = labGenerator.getEndPoint();
        float endAngle = labGenerator.getEndAngle();
        objEnd.setPosition(endPos[0], -0.5f, endPos[1]);
        objEnd.setRotation(endAngle, 'y');
        objEnd.updateModelM();

    }

    /**
     * Funzione che setta la dimensione dello SCISSOR e imposta i parametri della camera
     * ortogonale della mappa.
     *
     * Il bordo della mappa è creato andando a settare la camera ortogonale con dimensioni maggiori
     * del labirinto così da vedere un pezzo di sfondo per ciascun lato dando effetto bordo.
     * (quindi il colore del bordo è il colore di sfondo)
     *
     * Se larghezza lab >= altezza lab --> imposto larghezza mappa ad 1/5 della larghezza schermo
     * Se larghezza lab <  altezza lab --> imposto altezza mappa ad 1/2 dell'altezza schermo
     *
     * @param w Larghezza surface
     * @param h Altezza surface
     */
    public void setupProjection(int w, int h){

        float labDimX = (float) labGenerator.getDimension().x;
        float labDimY = (float) labGenerator.getDimension().y;

        float percentBorder = 3.0f;
        float border = (Math.max(labDimX, labDimY) / 100.0f) * percentBorder;
        float orthoX = (labDimX / 2) + border;
        float orthoY = (labDimY / 2) + border;

        cameraOrtho.setupProjection( -orthoX, orthoX, -orthoY, orthoY);
        cameraOrtho.updateViewAndPvM();

        float ratio = orthoX / orthoY;
        if (ratio>=1){
            // mappa landscape o square
            dimension.x = w / 5;
            dimension.y = (int) (dimension.x / ratio);
        }else{
            // mappa portrait
            dimension.y = h / 2;
            dimension.x = (int) (dimension.y * ratio);
        }

    }

    /**
     * Drawcalls per impostare lo SCISSOR e disegnare la mappa (pavimento + muri labirinto).
     *
     * @param currentScreen Dimensione schermo
     */
    public void drawMap2D(Point currentScreen){

        // (point_basso_sx, point_alto_dx)
        glScissor(currentScreen.x - dimension.x, 0, dimension.x, dimension.y);
        glViewport(currentScreen.x - dimension.x, 0, dimension.x, dimension.y);
        glClearColor(0, 0.45f, 0.9f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // il VAO bindato è del plane geometry (usato per floor e roof nel 3D) e usato anche
        // per disegnare gli oggetti della mappa

        objLabyrinthWalls[0].getMaterial().updateUniforms();      // aggiorno le uniform del primo e vanno bene per tutti i piani
        objLabyrinthWalls[0].getMaterial().activateTexture();  // tutti gli objLabyrinthWalls hanno la stessa texture quindi basta attivare quella del primo
        for ( Object3D obj : objLabyrinthWalls ){
            obj.draw(cameraOrtho);
        }

        objFloor.getMaterial().updateUniforms();
        objFloor.getMaterial().activateTexture();
        objFloor.draw(cameraOrtho);

    }

    /**
     * Drawcalls per disegnare le frecce di start e end nella mappa.
     */
    public void drawStartEndPointers(){

        // basta bindare il VAO del objStart in quanto è uguale a quello di objEnd
        GLES30.glBindVertexArray(objStart.getGeometry().getVao()[0]);

            objStart.getMaterial().updateUniforms();
            objStart.draw(cameraOrtho);

            objEnd.getMaterial().updateUniforms();
            objEnd.draw(cameraOrtho);

        GLES30.glBindVertexArray(0);

    }

    /**
     * Aggiorno la mappa con pasizione corrente della camera prospettica nel 3D.
     *
     * @param camera Camera prospettica nel 3D
     */
    public void updateFromCamera(CameraPersp3D camera){

        objStart.setRotation(camera.getRotationY(), 'y');
        objStart.setPosition(camera.getPosition());
        objStart.updateModelM();

    }

    @NonNull
    @Override
    public String toString(){

        StringBuilder res = new StringBuilder("Debug: \nMap:\nobjLabyrinthWalls:\n");

        for (int i=0; i<objLabyrinthWalls.length; i++){

            res.append("\tobj").append(i).append(": VAO=").append(objLabyrinthWalls[i].getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objLabyrinthWalls[i].getMaterial().getProgramId()).append("; TextureObjectID: ").append(objLabyrinthWalls[i].getMaterial().getTextureID()).append("\n");

        }

        res.append("objFloor: VAO=").append(objFloor.getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objFloor.getMaterial().getProgramId()).append("; TextureObjectID: ").append(objFloor.getMaterial().getTextureID()).append("\n");

        res.append("objStart: VAO=").append(objStart.getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objStart.getMaterial().getProgramId()).append("\n");

        res.append("objEnd: VAO=").append(objEnd.getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objEnd.getMaterial().getProgramId()).append("\n");

        return res.toString();
    }

}
