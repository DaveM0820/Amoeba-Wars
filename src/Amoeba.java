import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
public class Amoeba {

    /** array of vertices **/
    private final Vertex[] vertices;

    /** represents the velocity of the target center of the amoeba **/
    private Point3D velocity = new Point3D(0, 0, 0);

    /** strength of gravity **/
    private final double gravity = -0.0025;

    /** this is multiplied with velocity every frame to smooth the motion and create drag **/
    private final double dampening = 0.9;

    /** either 0 for player, 1 for food, or 2 for enemy **/
    private int type;

    /** a scalar that represents the speed this amoeba moves **/
    private double speed = 0.05;

    /** the group that the amoeba is drawn to **/
    private final Group draw;

    /** tracks whether the health is changing in order to make it flash if it's the player **/
    private boolean healthIsChanging = false;

    /** tracks whether health was changing but has stopped to reset the player colour back to blue **/
    private boolean healthIsChangingAnchor = false;

    /** a scalar for the size of the amoeba, works as the health **/
    private double hp = 1;

    /** initial radius of amoeba **/
    private final double initialRadius;

    /** initial speed of amoeba **/
    private final double initialSpeed;

    /** current target radius of amoeba **/
    private double radius;

    /** the target center point of the amoeba. All the vertices try to position themselves around this pont **/
    private Point3D targetCenter;

    /** this represents the "true" center of the amoeba, it's the average of all the vertices **/
    Point3D trueCenter;

    /** the sphere that represents the targetCenter, used for testing **/
    private final Sphere sphere;
    /**
     * Constructor for an Amoeba. It initializes instance variables, creates a sphere shape which will represent the center of the amoeba, and generates vertices in a sphere with a specified radius around the center.
     * finally
     * @param numberOfVertices the number of vertices this amoeba should have
     * @param radius the radius of this amoeba
     * @param draw the group to draw objects to.
     * @param center a point in 3d space representing the center of the amoeba
     */
    public Amoeba(int numberOfVertices, int radius, Group draw, Point3D center) {
        vertices = new Vertex[numberOfVertices];
        this.draw = draw;
        this.radius = radius;
        this.targetCenter = center;
        this.trueCenter = center;
        initialSpeed = speed;
        initialRadius = radius;

        //create a sphere that represents the center of the amoeba
        sphere = new Sphere(3, 20);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.rgb(90, 90, 240));
        sphere.setMaterial(material);
        sphere.setDrawMode(DrawMode.LINE); // Show only the outline of the box
        sphere.setTranslateX(targetCenter.getX());
        sphere.setTranslateY(targetCenter.getY());
        sphere.setTranslateZ(targetCenter.getZ());
        draw.getChildren().add(sphere);

        //Use the Fibonacci sphere algorithm to distribute the points evenly in a sphere. Really cool :)
        double goldenRatio = (1 + Math.sqrt(5)) / 2;
        double latitudeIncrement = Math.PI / numberOfVertices;
        double longitudeIncrement = 2 * Math.PI / goldenRatio;

        for (int i = 0; i < numberOfVertices; i++) {
            double theta = i * latitudeIncrement;
            double phi = i * longitudeIncrement;
            double x = radius * Math.sin(theta) * Math.cos(phi) + targetCenter.getX();
            double y = radius * Math.sin(theta) * Math.sin(phi) + targetCenter.getY();
            double z = radius * Math.cos(theta) + targetCenter.getZ();
            vertices[i] = new Vertex(i, new Point3D(x, y, z), draw, this, gravity, type);
        }
        for (Vertex currentVertex : vertices) {
            currentVertex.connectToAdjacentVertices();//for every vertex, connect it to nearby vertices
        }
        if (AmoebaWars.amoebas[0] != null) {
            if (radius < AmoebaWars.amoebas[0].getRadius()) { //if its radius is less than the player
                if (type != 1) { //but it's not food
                    changeAmoebaType(1);//turn it into food
                }
            } else { //if its radius is more than the player
                if (type != 1) { //but it's not an enemy
                    changeAmoebaType(2);//turn it into an enemy
                }
            }
        } else {
            changeAmoebaType(0);
        }
    }
    /**
     * returns an array of vertices for this amoeba
     */
    public Vertex[] getVertices() {
        return vertices;
    }
    /**
     * returns the 'target center' of the amoeba. This is the point that all vertices try to center themselves around.
     */
    public Point3D getTargetCenter() {
        return targetCenter;
    }
    /**
     * returns the true center of the amoeba. This is a point in 3d space calculated by averaging the position of all vertices
     */
    public Point3D getTrueCenter() {
        return trueCenter;
    }
    /**
     * returns the radius of the amoeba
     */
    public double getRadius() {
        return radius;
    }
    /**
     * returns the radius of the type of amoeba
     */
    public int getType() {
        return type;
    }
    /**
     * returns the current speed of the amoeba
     */
    public double getSpeed() {
        return speed;
    }
    /**
     * adds to the velocity of the amoeba, used when vertices apply force to the target center
     * velocityToAdd a Pont3D that represents the x,y, and z components of the velocity to add
     */
    public void addVelocity(Point3D velocityToAdd) { // used to add forces from the vertices to the amoeba center
        this.velocity = velocity.add(velocityToAdd);

    }
    /**
     * adds 'intentional' movement to the amoeba from the player, uses a frame counter to toggle the application of force on and off in order to
     * create a swimming animation
     * velocityToAdd a Pont3D that represents the x,y, and z components of the velocity to add
     * @param velocityToAdd a Point3D representing the velocity to add
     */
    public void addPlayerMovement(Point3D velocityToAdd) { // for intentional movement by the player. The frame counter causes the force to be turned on and off every 1.5 seconds, causing a swimming like animation
        int tempFrameCounter; // use a temp frame counter for values between 0 and 60
        if (AmoebaWars.frameCounter>90){
            tempFrameCounter = AmoebaWars.frameCounter - 90;
        } else{
            tempFrameCounter = AmoebaWars.frameCounter;
        }
        if (tempFrameCounter < 60) {
            this.velocity = velocity.add(velocityToAdd).multiply(speed * 100);
        }
    }
    /**
     * adds 'intentional' movement to the amoeba for non-player amoebas, uses a frame counter to toggle the application of force on and off in order to
     * create a swimming animation
     * velocityToAdd a Pont3D that represents the x,y, and z components of the velocity to add
     * @param velocityToAdd a Point3D representing the velocity to add
     */
    public void addNonPlayerMovement(Point3D velocityToAdd) { // for intentional movement by a non-player
        if (AmoebaWars.frameCounter %120 < 60) {
            this.velocity = velocity.add(velocityToAdd).multiply(speed * 100);
        }
    }
    /**
     * updates the speed of the amoeba based on it's size, and clamps speed within a 0.001 to 0.01 range
     */
    private void updateSpeed() {
        speed = 0.008 + ((1 - hp) / 200);//as it gets smaller it speeds up
        if (speed < 0.001) {//the slowest it can go is 0.03
            speed = 0.001;
        }
        if (speed > 0.01) {//the fastest any can go is 0.01
            speed = 0.01;
        }
        if (type == 1) { // but if it is food it's max speed is player speed * 0.4
            if (speed > AmoebaWars.amoebas[0].getSpeed() * 0.4) {
                speed = AmoebaWars.amoebas[0].getSpeed() * 0.4;
            }
        }
    }
    /**
     * When the amoeba dies, it's vertices, connections, and center become invisible, and it's center moves out of the place space
     */
    private void die(){
        for (Vertex vertex: vertices){
            vertex.changeVertexColour(-1);
        }
        sphere.setVisible(false);
        targetCenter = new Point3D(10000,10000,10000);
    }
    /**
     * averages all the vertex locations to find the center of the amoeba
     */
    private Point3D findTrueCenter() {
        double xSum = 0;
        double ySum = 0;
        double zSum = 0;
        for (Vertex vertex : vertices) {
            xSum += vertex.getLocation().getX();
            ySum += vertex.getLocation().getY();
            zSum += vertex.getLocation().getZ();
        }
        return new Point3D(xSum / vertices.length, ySum / vertices.length, zSum / vertices.length);
    }
    /**
     * the code to modify the HP of the amoeba, HP is analogous to amoeba size and is a value close to 1. When health is changed, the speed of the amoeba is changed
     * the amoeba is resized
     */
    private void changeHP(double amount) {
        healthIsChanging = true;//notes the health is changing
        healthIsChangingAnchor = true;//this is used to determine if health was changing, but has stopped changing, to make sure to reset the player colour back to blue
        if (radius > 6) {//if it's not the minimum size
            hp += amount;
            radius = initialRadius * hp; // hp is a value close to 1 which scales up and down hp, which scales up and down the size of the Amoeba
            updateSpeed();
            //System.out.println("new health " + hp + " new radius " + radius + " new speed " + speed);
            resizeAmoeba();
            if (this == AmoebaWars.amoebas[0]) {// if it's the player amoeba
                for (Amoeba amoeba : AmoebaWars.amoebas) {//go through every amoeba
                    if (amoeba != AmoebaWars.amoebas[0]) {// if the current one is not the player, compare size with the player, and if the size doesn't match the type (ie it's smaller but is an enemy) then change the type
                        if (amoeba.getType() == 2 && amoeba.getRadius() < AmoebaWars.amoebas[0].getRadius()) { //if its radius is less than the player, but it's an enemy
                            amoeba.changeAmoebaType(1);//turn it into food
                        } else if (amoeba.getType() == 1 && amoeba.getRadius() > AmoebaWars.amoebas[0].getRadius()) { //if its radius is greater than the player, but it's food
                            amoeba.changeAmoebaType(2);//turn it into an enemy
                        }
                    } else {//if it is the player make their colour flash green or red, indicating it they are taking damage or getting health
                        if (AmoebaWars.frameCounter % 20 == 0) {//if it's the 20th frame, change to appropriate colour
                            for (Vertex vertex : amoeba.getVertices()) {
                                if (amount > 0) {
                                    vertex.changeVertexColour(1);
                                } else {
                                    vertex.changeVertexColour(2);
                                }
                            }
                        } else if (AmoebaWars.frameCounter % 20 == 10) {//if it's the 10th frame, change back to blue
                            for (Vertex vertex : vertices) {
                                vertex.changeVertexColour(0);
                            }
                        }
                    }
                }
            }
        }else{//if it is less than 6 radius
            die();
        }
    }
    /**
     * code to changeAmoebaType
     *
     * @param newType the new amoeba type
     */
    private void changeAmoebaType(int newType) {//this will change the type of amoeba from food to enemy
        type = newType;
        for (Vertex vertex : vertices) {
            vertex.changeVertexColour(newType);//change the colour of the vertices to match the new type
        }
        updateSpeed();
        if (AmoebaWars.frameCounter>0) {//on the first frame the amoebas are null, this ensures win conditions aren't checked until they can be
            checkIfWinOrLose();
        }
    }
    /**
     * Checks if players has won or lost by seeing if all amoebas are red or green, and displays the appropriate message
     */
    private void checkIfWinOrLose() {
        //if all amoebas are enemies, you lose. if all amoebas are food, you're the biggest, and you win
        boolean allEnemy = true;
        boolean allFood = true;
        for (Amoeba amoeba : AmoebaWars.amoebas) {
                if (amoeba.getType() == 1) {
                    allEnemy = false;
                }
                if (amoeba.getType() == 2) {
                    allFood = false;
                }
        }
        if (allEnemy) {
            //you lose message
            AmoebaWars.displayLoseMessage();
        }
        if (allFood) {
            //you win message
            AmoebaWars.displayWinMessage();
        }
    }
    /**
     * resize the amoeba by moving vertices away from center
     */
    private void resizeAmoeba() {
        for (Vertex vertex : vertices) {
            vertex.moveVerticesAwayFromCenter(hp);//take the distance every vertex should be from the center and multiply it by hp
        }
    }
    /**
     * code to update the amoeba position, handles logic for food and enemies, and pushes amoebas away from each other if they intersect
     */
    public void updateAmoebaPosition() {
        healthIsChanging = false;
        velocity = velocity.add(new Point3D(0, -gravity, 0)); //subtract gravity from velocity
        velocity = velocity.multiply(dampening); //apply dampening
        trueCenter = findTrueCenter(); // calculate the average position of all vertices and assign it to the trueCenter variable
        if (trueCenter.getY() >= 0) { //if amoeba center hits the ground
            trueCenter = new Point3D(trueCenter.getX(), -1, trueCenter.getZ());//move it 1 unit above ground
            velocity = new Point3D(velocity.getX(), velocity.getY() * -1, velocity.getZ()); //reverse the y velocity
        }
        if (type != 0) {//for all non player amoebas, if two amoebas are within each other's radius, add a pushing force equal to three times the normal amoeba speed.
            for (Amoeba amoeba : AmoebaWars.amoebas) {
                if (amoeba != this && amoeba != AmoebaWars.amoebas[0]) {
                    double distance = trueCenter.distance(amoeba.getTrueCenter());
                    if (distance < radius + amoeba.getRadius()) {
                        velocity = velocity.subtract(amoeba.getTrueCenter().subtract(trueCenter).multiply(speed * 1.5 ));//if two amoebas are touching, push them apart
                    }
                }
            }
        } else { //if it's the player, check if the player is intersecting with another amoeba, if so add or subtract to the health of both amoebas
            for (Amoeba amoeba : AmoebaWars.amoebas) {
                if (amoeba != this) {
                    double distance = trueCenter.distance(amoeba.getTrueCenter());
                    if (distance < radius + (amoeba.getRadius()/2)) {
                        int typeOfAmoebaTouchingPlayer = amoeba.getType();
                        if (typeOfAmoebaTouchingPlayer == 1) { //if it's food
                            changeHP(0.0025*(2-AmoebaWars.difficulty));//the player gets big faster than the non-players
                            amoeba.changeHP(-0.0025*(2-AmoebaWars.difficulty));
                        } else {
                            changeHP(-0.00025*AmoebaWars.difficulty);
                            amoeba.changeHP(0.0025);
                        }
                    }
                }
            }
        }
        if (!healthIsChanging && healthIsChangingAnchor && this == AmoebaWars.amoebas[0]) {//if this is the player, the health isn't changing, but it was last frame, reset the anchor and change the player colour back to blue
            healthIsChangingAnchor = false;
            for (Vertex vertex : vertices) {
                vertex.changeVertexColour(0);
            }
        }
        if (type == 1) { // if it's food try to keep a safe distance away from the player, but don't go too close to the floor
            Point3D targetPosition = AmoebaWars.amoebas[0].getTargetCenter();
            double distance = trueCenter.distance(targetPosition);
            if (distance < 200) {
                addNonPlayerMovement(targetPosition.subtract(targetCenter).normalize().multiply(speed * -250 * AmoebaWars.difficulty));
            } else {
                addNonPlayerMovement(targetPosition.subtract(targetCenter).normalize().multiply(speed * 200 * AmoebaWars.difficulty));
            }
            if (trueCenter.getY() < -200) {
                velocity.add(new Point3D(0, 5, 0));
            }
        }
        if (type == 2) { // if it's an enemy move towards the player
            addNonPlayerMovement(AmoebaWars.amoebas[0].getTrueCenter().subtract(trueCenter).normalize().multiply(speed * 60 * AmoebaWars.difficulty));//this takes the normalized directional vector and multiply it by speed
        }
        targetCenter = targetCenter.add(velocity.multiply(AmoebaWars.timeScale)); //add current velocity to the target center point
        sphere.setTranslateX(trueCenter.getX());
        sphere.setTranslateY(trueCenter.getY());
        sphere.setTranslateZ(trueCenter.getZ());
        for (Vertex vertex : vertices) {
            vertex.applyForces(); // for each vertex apply all necessary forces, such as spring forces to the connected vertices and gravity
        }
    }

}
