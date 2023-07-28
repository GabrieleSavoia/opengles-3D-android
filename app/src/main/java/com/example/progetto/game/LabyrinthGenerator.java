package com.example.progetto.game;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Classe per la generazione del labirinto.
 */
public class LabyrinthGenerator {

    private static String TAG;

    private final Point dimension;
    private int[][] labyrinth;

    private Point startP;
    private float startAngle;
    private Point endP;
    private float endAngle;

    /**
     * Costruttore della classe per la generazione del labirinto.
     *
     * @param dimension Dimensioni del labirinto:
     *                      - se pari, verranno incrementate di 1 unità per renderle dispari;
     *                      - dimensione almeno 5x5: imposto 5x5 se è fornita una dimensione minore;
     */
    public LabyrinthGenerator(Point dimension){

        TAG = getClass().getSimpleName();

        this.dimension = new Point(dimension.x, dimension.y);

        // se troppo piccolo, imposto 5x5
        if (this.dimension.x < 5) this.dimension.x = 5;
        if (this.dimension.y < 5) this.dimension.y = 5;

        // se sono pari incremento di 1 unità
        if (this.dimension.x % 2 == 0) this.dimension.x += 1;
        if (this.dimension.y % 2 == 0) this.dimension.y += 1;

        labyrinth = null;

        startP = new Point(0, 0);
        startAngle = 0f;
        endP = new Point(0, 0);
        endAngle = 0f;

    }

    /**
     * Funzione per la generazione di un labirinto (con bordo esterno) con algoritmo AldousBroder.
     * Le dimensioni in input devono essere dispari.
     *
     * (Vedi figura sotto)
     * Considero come punto iniziale 'S' un punto random tra gli 1 della penultima riga.
     * (es. nella figura in basso 'S' = (7,3)).
     *
     * Una volta creato il labirinto, considero come punto finale di arrivo un punto
     * a caso tra gli '1' della riga 1.
     *
     * Ho garanzia che esista un path percorribile tra il punto iniziale e quello finale perchè
     * fanno entrambi parte dello stesso Spanning Tree.
     *
     * LINK: - https://github.com/john-science/mazelib/blob/main/mazelib/generate/AldousBroder.py
     *       - https://github.com/john-science/mazelib/blob/main/docs/MAZE_GEN_ALGOS.md
     *
     * Esempio labirinto 9x9:
     *      Dato 'S' come punto di partenza, viene generato un labirinto che connette S con tutti i
     *      punti '1' andando ad abbattere i muri '0' necessari :
     *
     *      0 0 0 0 0 0 0 0 0             GRAFO 'G'          UNIFORM SPANNING TREE  (esempio)
     *      0 1 0 1 0 1 0 1 0            1 - 1 - 1 - 1           1 - 1 - 1 - 1
     *      0 0 0 0 0 0 0 0 0            |   |   |   |                       |
     *      0 1 0 1 0 1 0 1 0            1 - 1 - 1 - 1           1 - 1 - 1 - 1
     *      0 0 0 0 0 0 0 0 0    --->    |   |   |   |   --->        |
     *      0 1 0 1 0 1 0 1 0            1 - 1 - 1 - 1           1 - 1 - 1 - 1
     *      0 0 0 0 0 0 0 0 0            |   |   |   |           |       |
     *      0 1 0 S 0 1 0 1 0            1 - S - 1 - 1           1 - S   1 - 1
     *      0 0 0 0 0 0 0 0 0
     *
     * Come funziona:
     *      - suppongo che la connessione tra gli '1' formi il grafo 'G';
     *      - trovo uno Uniform Spanning Tree di G e definisco il labirinto andando ad
     *        abbattere i muri '0' in corrispondenza degli archi dello Spanning Tree.
     *      - 'uniform' perchè il processo di generazione è casuale con prob. uniforme.
     *
     * Ad esempio se 'S' = (7,3) e decido di mettere nello Spanning Tree l'arco che connette 'S'
     * ed il nodo '1' = (7, 1) alla sua sinistra, allora questo significa abbattere il
     * muro '0' = (7, 2).
     *
     * IMPORTANTE: il punto iniziale 'S' deve essere in coordinate dispari.
     *
     */
    public void generate(){

        int row = dimension.y;
        int col = dimension.x;

        labyrinth = new int [row][col];

        for (int i=0; i<row; i++){
            for (int z=0; z<col; z++){
                labyrinth[i][z] = 0;     // tutti muri
            }
        }

        Random random = new Random();

        // trovo punto iniziale
        int randomCol = random.nextInt(col-1 );  // col-2 compreso è dispari
        if (randomCol % 2 == 0) randomCol += 1;
        startP = new Point(row-2, randomCol);

        Point currPoint = startP;
        labyrinth[currPoint.x][currPoint.y] = 1;
        int numVisited = 1;

        // tutti i nodi '1' devono essere visitati (es. con dim_lab=9x9 ho 4*4 nodi '1' da visitare)
        int totalToVisit = ( ((row-1)/2) * ((col-1)/2) );

        while (numVisited < totalToVisit){

            // trovo tutti i punti vicini non ancora visitati (sono ancora a zero)
            ArrayList<Point> neigh = getNeighbours(currPoint, 0);

            // Se tutti i vicini sono stati visitati, metto come current un vicino random visitato
            if (neigh.size() == 0){

                ArrayList<Point> neighVisited = getNeighbours(currPoint, 1);

                // zero compreso, neighVisited.size() escluso
                currPoint = neighVisited.get(random.nextInt(neighVisited.size()));

                // continua il while con l'iterazione successiva
                continue;

            }

            // itero per i vicini non ancora visitati
            for (Point pointNeigh: neigh){

                if ( labyrinth[pointNeigh.x][pointNeigh.y] == 0 ){

                    // abbatto (metto a 1) il muro che c'è tra il punto corrente e il vicino
                    int wallToRemoveX = (int) Math.floor( (pointNeigh.x+currPoint.x) / 2f );
                    int wallToRemoveY = (int) Math.floor( (pointNeigh.y+currPoint.y) / 2f );
                    labyrinth[ wallToRemoveX ][ wallToRemoveY ] = 1;

                    // visito il vicino e aggiorno il corrente al vicino
                    labyrinth[ pointNeigh.x ][ pointNeigh.y ] = 1;
                    numVisited += 1;
                    currPoint = pointNeigh;

                    break;

                }

            }

        }

        // Printo labirinto per debug
        StringBuilder lab = new StringBuilder("Labirinto: \n");
        for (int i=0; i<row; i++){
            lab.append("[");
            for (int z=0; z<col; z++){
                lab.append(" ").append(labyrinth[i][z]);
            }
            lab.append(" ]\n");
        }
        Log.d(TAG, lab.toString());

        // START : ultima riga
        startP = new Point(startP.x+1, startP.y);
        startAngle = 0;
        labyrinth[startP.x][startP.y] = 1;

        // END : prima riga con colonna a caso (dispari)
        randomCol = random.nextInt(col-1 );
        if (randomCol % 2 == 0) randomCol += 1;
        endP = new Point(0, randomCol);
        endAngle = 180;
        labyrinth[endP.x][endP.y] = 1;

    }

    /**
     * Dato un punto 'p' ritorna una lista di vicini ( al più 4: Nord, Est, Sud, Ovest) che hanno
     * un certo valore 'val' nella matrice.
     *
     * IMPORTANTE: i vicini non sono adiacenti ma sono separati da 1 unità in tutte le direzioni.
     *              E' per questo che si incrementa e decrementa di 2.
     *
     * @param p Punto di cui si vuole conoscere i vicini
     * @param val Valore che devono avere i vicini. (es. val=1 permette di ritornare tutti e soli i
     *            vicini con che hanno valore 1 nella matrice)
     * @return ArrayList di Point che rappresentano i vicini di 'p' aventi un certo valore 'val'
     */
    private ArrayList<Point> getNeighbours(Point p, int val){

        ArrayList<Point> neigh = new ArrayList<>();

        int row = dimension.y;
        int col = dimension.x;

        // nord
        if ( (p.x > 1) && (labyrinth[p.x-2][p.y] == val) ){
            neigh.add( new Point(p.x-2,  p.y));
        }
        // sud
        if ( (p.x < row-2) && (labyrinth[p.x+2][p.y] == val) ){
            neigh.add( new Point(p.x+2,  p.y));
        }
        // est
        if ( (p.y > 1) && (labyrinth[p.x][p.y-2] == val) ){
            neigh.add( new Point(p.x,  p.y-2));
        }
        // ovest
        if ( (p.y < col-2) && (labyrinth[p.x][p.y+2] == val) ){
            neigh.add( new Point(p.x,  p.y+2));
        }

        Collections.shuffle(neigh);

        return neigh;

    }

    /**
     * Funzione che converte le coordinate nello spazio 3D (considero solo 'x' e 'z') con gli indici
     * relativi alla matrice del labirinto.
     *
     * @param x Coordinata 'x' nello spazio 3D
     * @param z Coordinata 'y' nello spazio 3D
     * @return Indici (indice_riga, indice_colonna) della matrice del labirinto
     */
    public int[] fromCoordToIndices(float x, float z){

        float dimX = (float) dimension.x;
        float dimY = (float) dimension.y;

        int[] res = new int[2];

        res[0] = (int) (z + (dimY / 2) - 0.5f);    // indice della riga
        res[1] = (int) (x + (dimX / 2) - 0.5f);    // indice della colonna

        return res;

    }

    /**
     * Funzione che controlla se una certa coordianta nello spazio 3D (considero solo 'x' e 'z') è
     * camminabile.
     *
     * @param x Coordinata 'x' nello spazio 3D
     * @param z Coordinata 'z' nello spazio 3D
     * @return True se la coordianta è camminabile, False altrimenti.
     */
    public boolean isWalkable(float x, float z){

        int[] indices = fromCoordToIndices(x, z);

        if ( (indices[0] < 0) || (indices[1] < 0) ||
             (indices[0] >= dimension.y) || (indices [1] >= dimension.x) ){

            return false;
        }

        return labyrinth[indices[0]][indices[1]] != 0;

    }

    /**
     * Funzione che converte gli indici della matrice del labirinto in coordinate nello spazio 3D
     * (considero solo 'x' e 'z').
     *
     * @param row Indice di riga della matrice del labirinto
     * @param col Indice di colonna della matrice del labirinto
     * @return Coordinata ('x', 'z') nello spazio 3D
     */
    public float[] fromIndicesToCoord(int row, int col){

        float[] res = new float[2];

        float dimX = (float) dimension.x;
        float dimY = (float) dimension.y;

        float x = (float) col;
        float y = (float) row;

        res[0] = x - (dimX / 2) + 0.5f;
        res[1] = y - (dimY / 2) + 0.5f;

        return res;

    }

    /**
     * Funzione che ritorna le coordinate 3D ('x', 'z') di ogni parete del labirinto.
     *
     * @return Lista [ ['x', 'z'], ['x', 'z'], ... ] con le coordinate 3D delle pareti del labirinto
     */
    public float[][] getWallsCoord(){

        int row = dimension.y;
        int col = dimension.x;

        float[][] res = new float [getNumWall()][2];  // [ [x,z], [x,z], ... ]

        int count = 0;

        for (int i=0; i<row; i++){
            for (int z=0; z<col; z++){

                if (labyrinth[i][z] == 0){   // se è wall

                    float[] coord = fromIndicesToCoord(i, z);
                    res[count] = new float[] { coord[0], coord[1] };

                    count++;

                }
            }
        }

        return res;

    }

    /**
     * Funzione che ritorna il numero di pareti totali nel labirinto.
     *
     * @return Numero di pareti totali nel labirinto
     */
    public int getNumWall(){

        int row = dimension.y;
        int col = dimension.x;

        int sum = 0;

        for (int i=0; i<row; i++){
            for (int z=0; z<col; z++){

                if (labyrinth[i][z] == 0){
                    sum++;
                }

            }
        }

        return sum;

    }

    /********** GETTERS ***********/

    public float[] getStartPoint(){ return fromIndicesToCoord(startP.x, startP.y); }

    public float[] getEndPoint(){ return fromIndicesToCoord(endP.x, endP.y); }

    public float getStartAngle(){ return startAngle; }

    public float getEndAngle(){ return endAngle; }

    public Point getDimension() { return dimension; }

}
