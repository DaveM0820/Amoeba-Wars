import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.Arrays;

public class Vertex {
    /**
     * The location of this vertex
     */
    private Point3D location;

    /**
     * The parent amoeba
     */
    private final Amoeba amoeba;

    /**
     * Representing velocity as a vector rather than three separate variables because it's easier to add and subtract, and looks cleaner
     */
    private Point3D velocity;

    /**
     * This is its target position relative to the center of the amoeba, added this in after it became apparent that the connections to other vertices and to the center wasn't enough to stop the points from bunching up when it moves.
     */
    private Point3D targetDeltaFromCenter;

    /**
     * The initial target position relative to the center
     */
    private Point3D initialTargetDeltaFromCenter;

    /**
     * Used to scale the spring force between vertices
     */
    private final double springScalar = 0.001;

    /**
     * Used to scale the force pushing from the center of the amoeba
     */
    private final double centerSpringScalar = 0.0006;


    /**
     * Velocity is multiplied by this number every frame, acting as drag
     */
    private final double dampening = 0.960;

    /**
     * The distance at which the spring force won't be active
     */
    private final double tolerance = 5;

    /**
     * The force of gravity, subtracted from y velocity every frame
     */
    private final double gravity;

    /**
     * Represents the index of this vertex
     */
    private final int vertexID;

    /**
     * The group to draw things to
     */
    private final Group draw;

    /**
     * The sphere representing the vertex
     */
    private final Sphere sphere;

    /**
     * The lines between vertices
     */
    Cylinder[] connectionLines = new Cylinder[6];

    /**
     * Each vertex connects to 6 others, the 4 closest and 2 random
     */
    private final Vertex[] connections = new Vertex[6];

    /**
     * The desired distance between the vertices
     */
    private final double[] targetDistance = new double[6];

    /**
     * This takes the index of the vert and normalizes it. so it's within the range of the values of the frame counter, this way it can be used to distribute expensive processes(like resizing cylinders) over the course of many frames
     */
    int normalizedIndexForFrameCounter;

    /**
     * Constructor for the vertex, it initializes instance variables and creates the sphere that represents that vertex
     *
     * @param vertexID the index of that vertex in the vertices array of the amoeba
     * @param location the point in 3d space that represents the vertex location
     * @param draw     the group to draw the sphere and connection lines to
     * @param amoeba   the parent amoeba
     * @param gravity  the force of gravity to apply to this vertex
     * @param type     the type of amoeba this vertex belongs to
     */

    public Vertex(int vertexID, Point3D location, Group draw, Amoeba amoeba, double gravity, int type) {
        velocity = new Point3D(0, 0, 0);
        this.gravity = gravity;
        this.vertexID = vertexID;
        this.amoeba = amoeba;
        this.location = location;
        this.draw = draw;
        normalizedIndexForFrameCounter = (int) (vertexID / 200) * 180;//there are max 200 vertices and max 180 frames in the frame counter, this puts the vert number within the 180 frames of the frame counter
        sphere = new Sphere(2, 10);
        changeVertexColour(type);
        sphere.setTranslateX(location.getX());
        sphere.setTranslateY(location.getY());
        sphere.setTranslateZ(location.getZ());
        draw.getChildren().add(sphere);
    }

    /**
     * The code to change the vertex colour based on which type it is
     *
     * @param type the type of amoeba this vertex belongs to
     */
    public void changeVertexColour(int type) {
        if (type == 0) {
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.rgb(50, 50, 240));
            sphere.setMaterial(material);
        } else if (type == 1) {
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.rgb(90, 240, 90));
            sphere.setMaterial(material);
        } else if (type == 2) {
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.rgb(240, 90, 90));
            sphere.setMaterial(material);
        } else {
            sphere.setVisible(false);
            for (Cylinder cylinder : connectionLines) {
                cylinder.setVisible(false);
            }
        }

    }

    /**
     * This changes the distance of the relative position to the center by a specified factor
     *
     * @param scaleFactor how much to scale up or down the amoeba
     */

    public void moveVerticesAwayFromCenter(double scaleFactor) {
        targetDeltaFromCenter = initialTargetDeltaFromCenter.multiply(scaleFactor);
    }

    /**
     * returns the location of this vertex
     */
    public Point3D getLocation() {
        return location;
    }

    /**
     * returns the connections this vertex has to others
     */
    public Vertex[] getConnections() {
        return connections;
    }

    /**
     * changes a vertex connection
     *
     * @param index  the index of the connection to change
     * @param vertex the new vertex to connect to
     */
    public void setConnection(int index, Vertex vertex) {
        connections[index] = vertex;
    }

    /**
     * checks if one vertex equals this one
     *
     * @param vertex the vertex to compare
     */
    public boolean equals(Vertex vertex) {
        return vertex == this;
    }

    /**
     * This calculates all the forces that act on this vertex, including connections to other vertices, distance to center, gravity, dampening, and difference between current and relative position to center
     */
    public void applyForces() {
        int counter = 0;
        for (Vertex currentVertex : connections) { //for all the vertices this one is connected to
            if (counter < 3) {//temporarily disabled connections to random vertices(vertices 4 and 5) because they cause the amoeba to become unstable when it gets too big
                double distance = location.distance(currentVertex.getLocation()); //calculate the distance between this instance and the others
                Point3D positionDelta = location.subtract(currentVertex.getLocation()); //find the difference in position
                if (distance - targetDistance[counter] > tolerance && targetDistance[counter] - distance < tolerance) { // check if it's out of bounds of the tolerance
                    if (distance < targetDistance[counter]) { //if the distance is less than the desired distance
                        velocity = velocity.add(positionDelta.multiply(springScalar)); //find the difference between the points, multiply it by springScalar, and add that number to velocity
                    } else {
                        if (distance < targetDistance[counter] / 2) { // if the distance is less than half the target distance, ie the points are way too close together
                            velocity = velocity.subtract(positionDelta.multiply(springScalar * 10));//multiply the scalar by 10
                        } else {
                            velocity = velocity.subtract(positionDelta.multiply(springScalar));//otherwise keep the scalar the same
                        }
                    }
                }

                redrawLine(connectionLines[counter], currentVertex.getLocation(), location);//redraw the connection between vertices
            }
            counter++;
        }
        //apply the spring force to the center
        double distance = location.distance(amoeba.getTargetCenter());
        Point3D positionDelta = location.subtract(amoeba.getTargetCenter());
        if (distance < amoeba.getRadius()) { //if the distance is less than it should be
            velocity = velocity.add(positionDelta.multiply(centerSpringScalar)); //add the difference between the points * spring scalar to velocity
            amoeba.addVelocity(positionDelta.multiply(-centerSpringScalar * 0.2));//apply opposite force to the amoeba target center
        } else {
            velocity = velocity.subtract(positionDelta.multiply(centerSpringScalar));//otherwise subtract the difference
            amoeba.addVelocity(positionDelta.multiply(centerSpringScalar * 0.5));//and apply force to the amoeba target center
        }
        if (location.getY() >= 0) { //if this vert hits the ground
            location = new Point3D(location.getX(), -1, location.getZ());//move it 1 unit above ground
            velocity = new Point3D(velocity.getX(), velocity.getY() * -1, velocity.getZ()); //reverse the y velocity
            amoeba.addVelocity(new Point3D(0, velocity.getY() * 0.6, 0));//add opposite force to the amoeba target center
        }
        velocity = velocity.add(targetDeltaFromCenter.subtract(location.subtract(amoeba.getTargetCenter())).multiply(0.001)); //find where it is relative to the center, where it should be relative to the center, take the difference of those two, multiply it by a scalar, and add it to velocity. Basically, make it try to stay in the same position relative ot the center.
        velocity = velocity.subtract(new Point3D(0, gravity, 0)); //subtract gravity from velocity
        velocity = velocity.multiply(dampening); // apply dampening
        location = location.add(velocity.multiply(AmoebaWars.timeScale));//add the velocity to location
        sphere.setTranslateX(location.getX());//move the sphere that represents the vertex
        sphere.setTranslateY(location.getY());
        sphere.setTranslateZ(location.getZ());
    }

    /**
     * adjacent
     * find 3 closest points and connect them
     * after that connect three random points. Without the three random points the vertices tend to slide along the surface of the sphere, causing it to lose shape
     * after that, find the position relative to the center and assign it to a variable. The vertex will have the delta between this point, and it's position added to velocity.  This is to further reduce hte likelihood of points bunching up on one side of the amoeba when force is applied.
     */
    public void connectToAdjacentVertices() {
        for (int i = 0; i < 3; i++) {//search for the closest point 3 times
            double minDistance = amoeba.getRadius();
            int currentIndex = -1;
            int indexCounter = 0;
            for (Vertex currentVertex : amoeba.getVertices()) {

                if (Arrays.stream(connections).noneMatch(vertex -> currentVertex.equals(vertex)) && currentVertex != this) {//if the vertex current being assessed is not this vertex or one it's already connected to
                    double distance = location.distance(currentVertex.getLocation().getX(), currentVertex.getLocation().getY(), currentVertex.getLocation().getZ());//calculate the distance between the two
                    if (distance < minDistance) {//if it's less than the min distance assign it as the new closest point
                        minDistance = distance;
                        currentIndex = indexCounter;
                        connections[i] = currentVertex;
                        targetDistance[i] = distance;
                    }
                }
                indexCounter++;
            }
            //this excessively long line just creates a cylinder that connects two vertices
            connectionLines[i] = createLine(new Point3D(location.getX(), location.getY(), location.getZ()), new Point3D(amoeba.getVertices()[currentIndex].getLocation().getX(), amoeba.getVertices()[currentIndex].getLocation().getY(), amoeba.getVertices()[currentIndex].getLocation().getZ()));
            draw.getChildren().add(connectionLines[i]);//add the line to the connection lines for this vert
            amoeba.getVertices()[currentIndex].setConnection(amoeba.getVertices()[currentIndex].getConnections().length - 1, this);//set the connection for the vertex this one connects to as well, saves some time by not doing it twice
            //System.out.println("vertex " + vertexID + " position " + i +  " connected to " + currentIndex);
        }
        int numberOfRandomConnections = 3;//how many random connections to add
        for (int i = 0; i < numberOfRandomConnections; i++) {//add three random connections, this is to create forces within the sphere that help keep the points in the same position relative to each other
            int randomVert = 1 + (int) (Math.random() * amoeba.getVertices().length - 1);
            connections[numberOfRandomConnections + i] = amoeba.getVertices()[randomVert];
            targetDistance[numberOfRandomConnections + i] = location.distance(connections[numberOfRandomConnections + i].getLocation().getX(), connections[numberOfRandomConnections + i].getLocation().getY(), connections[numberOfRandomConnections + i].getLocation().getZ());//set the target distance between the points
            connectionLines[numberOfRandomConnections + i] = createLine(new Point3D(location.getX(), location.getY(), location.getZ()), new Point3D(connections[numberOfRandomConnections + i].getLocation().getX(), connections[numberOfRandomConnections + i].getLocation().getY(), connections[numberOfRandomConnections].getLocation().getZ()));//create a new line between the two vertices
        }
        //find the initial difference in position from the center
        targetDeltaFromCenter = location.subtract(amoeba.getTargetCenter());
        initialTargetDeltaFromCenter = targetDeltaFromCenter;

    }

    /**
     * creates the cylinder that's used to draw the line between vertices
     *
     * @param from the starting point
     * @param to   the ending point
     */
    public Cylinder createLine(Point3D from, Point3D to) {
        // define a point representing the Y axis
        Point3D yAxis = new Point3D(0, 1, 0);
        // find the difference between the points
        Point3D seg = from.subtract(to);
        // determine the length of the line/height of our cylinder object
        double height = seg.magnitude();
        // get the midpoint of the line
        Point3D midpoint = from.midpoint(to);
        // set up a translation to move to the cylinder to the midpoint
        Translate moveToMidpoint = new Translate(midpoint.getX(), midpoint.getY(), midpoint.getZ());
        // find the axis about which we want to rotate the cylinder
        Point3D axisOfRotation = seg.crossProduct(yAxis);
        // find the angle we want to rotate the cylinder
        double angle = Math.acos(seg.normalize().dotProduct(yAxis));
        // create the rotating transform
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
        // create the cylinder object representing the line
        Cylinder line = new Cylinder(0.4, height, 4);
        // add the transfroms to the cylinder

        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        //line.setDrawMode(DrawMode.LINE);
        // return the cylinder for use
        return line;
    }

    /**
     * re-draws the cylinder that's used to draw the line between vertices
     *
     * @param cylinder the cylinder to modify
     * @param from     the starting point
     * @param to       the ending point
     */
    public void redrawLine(Cylinder cylinder, Point3D from, Point3D to) { // same as createCylinder but this just redraws it
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D seg = from.subtract(to);
        double height = seg.magnitude();
        Point3D midpoint = from.midpoint(to);
        Translate moveToMidpoint = new Translate(midpoint.getX(), midpoint.getY(), midpoint.getZ());
        Point3D axisOfRotation = seg.crossProduct(yAxis);
        double angle = Math.acos(seg.normalize().dotProduct(yAxis));
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
        if (AmoebaWars.frameCounter == normalizedIndexForFrameCounter) { // setting the cylinder height causes a big hit to performance so only do it once per 180 frames per cylinder. Which frame depends on the vert index
            cylinder.setHeight(height); //cause huge performance drop
        }
        cylinder.getTransforms().clear();
        cylinder.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
    }
}
