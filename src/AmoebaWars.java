import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class AmoebaWars extends Application {

    /**how fast the camera rotates around the point**/
    private final double cameraSpeed = 0.5;
    /**default camera distance**/
    private double cameraDistance = 400;
    /**maximum camera distance**/
    private final double maxCameraDistance = 1000;
    /**minimum camera distance**/
    private final double minCameraDistance = 30;
    /**Is the player moving forwards, controlled by keyboard**/
    private boolean movingForward = false;
    /**Is the player moving backward, controlled by keyboard**/
    private boolean movingBackward = false;
    /**Is the player moving left, controlled by keyboard**/
    private boolean movingLeft = false;
    /**Is the player moving right, controlled by keyboard**/
    private boolean movingRight = false;
    /**Is the player moving up**/
    private boolean movingUp = false;
    /**Is the player moving down**/
    private boolean movingDown = false;
    /**current mouse x position**/
    private double mouseX = 0;
    /**current mouse y position**/
    private double mouseY = 0;
    /**mouse x position in previous frame**/
    private double previousMouseX = 0;
    /**mouse y position in previous frame**/
    private double previousMouseY = 0;
    /**x position of camera**/
    private double cameraX = 0;
    /**y position of camera**/
    private double cameraY = 0;
    /**z position of camera**/
    private double cameraZ = 0;
    /**mouse movement in x direction **/
    private double mouseXMovement = 0;
    /**mouse movement in y direction**/
    private double mouseYMovement = 0;
    /**this is a counter that counts up to 60 and resets **/
    public static int frameCounter;
    /**array of the amoebas in the game**/
    public static Amoeba[] amoebas;
    /**The UI that displays the controls, JavaFX 3D doesn't have a way to directly overlay 2D UI, so we draw a box in front of the camera and texture the box with UI images**/
    private Box UIControls;
    /**The UI that displays the instructions**/
    private Box UIInstructions;
    /**Starting, winning, and losing message, static because the amoebas need to accesses it in win/lose conditions
     **/
    public static Box UIContext;
    /**a scalar for all velocities. It's applied before adding velocity to position. Used to speed up or slow down time, values above 3 cause issues with the physics calculations**/
    public static double timeScale = 0;
    /**the point that the camera faces, follow the player amoeba**/
    Point3D cameraTarget;
    /**the velocity of the camera target**/
    Point3D cameraTargetVelocity = new Point3D(0, 0, 0);
    /**the material that will hold the texture for the intro message**/
    public static PhongMaterial UIIntroTexture;
    /**the material that will hold the texture for the win message**/
    public static PhongMaterial UIWinTexture;
    /**the material that will hold the texture for the lose message **/
    public static PhongMaterial UILoseTexture;
    /** a value that modifies the speed of enemies/food **/
    public static double difficulty;//a value that modifies the speed of enemies/food
    /**
     * The method which sets up the scene and initializes important variable
     * @param primaryStage the stage for this scene
     */
    @Override
    public void start(Stage primaryStage) {
        // instantiate the root group
        Group root = new Group();
        // instantiate the scene
        Label label1= new Label("Please select the difficulty");
        Button button= new Button("Begin!");
        Scene scene = new Scene(root, 1300, 1000, true);
        Slider slider = new Slider(0.2, 2, 1);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(0.25f);
        slider.setBlockIncrement(0.1f);
        button.setOnAction(e -> {
            if (e.getSource() == button) {
                primaryStage.setScene(scene);
                difficulty= slider.getValue();
            }
        });
        VBox layout1 = new VBox(20);
        label1.setTranslateX(80);
        button.setTranslateX(120);
        label1.setTranslateY(20);
        slider.setTranslateY(30);
        button.setTranslateY(40);

        layout1.getChildren().addAll(label1, slider, button);

        Scene titleScreenScene = new Scene(layout1, 300, 200);


        primaryStage.setTitle("Amoeba Wars");
        primaryStage.setScene(titleScreenScene);


        PerspectiveCamera camera = new PerspectiveCamera(true);//instantiate a new camera
        camera.setFarClip(10000);// set the far clip plane
        cameraTarget = new Point3D(0, -100, 0);//initialize the camera target location

        //create the  new boxes that will display the controls, instructions, and context based messages, place them in front of the camera
        UIControls = new Box(7.5, 5, 0.01);
        UIInstructions = new Box(8, 5, 0.01);
        UIContext = new Box(12, 12, 0.01);
        UIControls.setTranslateZ(50);
        UIControls.setTranslateX(-13);
        UIControls.setTranslateY(-10);
        UIInstructions.setTranslateZ(50);
        UIInstructions.setTranslateX(13);
        UIInstructions.setTranslateY(-10);
        UIContext.setTranslateZ(50);
        UIContext.setTranslateX(0);
        UIContext.setTranslateY(0);

        //create materials with textures and assign them to the boxes
        PhongMaterial UIControlsTexture = new PhongMaterial();
        UIControlsTexture.setDiffuseMap(new Image("controls.png"));
        PhongMaterial UIInstructionsTexture = new PhongMaterial();
        UIInstructionsTexture.setDiffuseMap(new Image("instructions.png"));
        UIIntroTexture = new PhongMaterial();
        UIIntroTexture.setDiffuseMap(new Image("intro.png"));
        UIWinTexture = new PhongMaterial();
        UIWinTexture.setDiffuseMap(new Image("win.png"));
        UILoseTexture = new PhongMaterial();
        UILoseTexture.setDiffuseMap(new Image("lose.png"));
        UIControls.setMaterial(UIControlsTexture);
        UIInstructions.setMaterial(UIInstructionsTexture);
        UIContext.setMaterial(UIIntroTexture);

        //put the UI and camera together in a group, when the camera moves the movements will be applied to the whole group
        Group cameraAndUI = new Group(camera, UIControls, UIInstructions, UIContext);
        root.getChildren().add(cameraAndUI); // Add the camera/UI group to the root group

        amoebas = new Amoeba[8]; //create an array of Amoebas
        amoebas[0] = new Amoeba(200, 25, root, new Point3D(0, -100, 0));//player
        amoebas[1] = new Amoeba(60, 50, root, new Point3D(0, -300, 400));
        amoebas[2] = new Amoeba(60, 20, root, new Point3D(-400, -400, 400));
        amoebas[3] = new Amoeba(60, 20, root, new Point3D(200, -100, -200));
        amoebas[4] = new Amoeba(60, 10, root, new Point3D(-200, -300, 200));
        amoebas[5] = new Amoeba(60, 40, root, new Point3D(280, -100, -100));
        amoebas[6] = new Amoeba(60, 26, root, new Point3D(-350, -300, 350));
        amoebas[7] = new Amoeba(60, 24, root, new Point3D(-200, -300, 120));

        // Set up the scene
        //make the floor
        Box box = new Box(2000, 1, 2000); // Create a cube
        root.getChildren().add(box); // Add the cube to the scene graph

        // create a grid
        int gridSize = 20;
        int cellSize = 2000;
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                for (int z = 0; z < gridSize; z++) {
                    box = new Box(cellSize, cellSize, cellSize); // Create a cube
                    box.setDrawMode(DrawMode.LINE); // Show only the outline of the box
                    box.setTranslateX(x * cellSize - (cellSize * gridSize * 0.5)); // Position the cube in the X direction
                    box.setTranslateY(y * cellSize - (cellSize * gridSize * 0.5)); // Position the cube in the Y direction
                    box.setTranslateZ(z * cellSize - (cellSize * gridSize * 0.5)); // Position the cube in the Z direction
                    root.getChildren().add(box); // Add the cube to the scene graph
                }
            }
        }

        //create randomly placed balls to give a sense of space
        for (int x = 0; x < 2000; x++) {
            Sphere sphere = new Sphere((int) (0.2 + Math.random() * 5), (int) (1 + Math.random() * 5)); // Create random spheres
            sphere.setTranslateX(Math.random() * 1000 - 500);
            sphere.setTranslateY(Math.random() * 1000 - 500);
            sphere.setTranslateZ(Math.random() * 1000 - 500);
            root.getChildren().add(sphere); // Add the cube to the scene graph
        }

        scene.setOnMouseMoved((MouseEvent event) -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });

        // Handle keyboard events
        scene.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.W) {
                movingForward = true;
            } else if (event.getCode() == KeyCode.S) {
                movingBackward = true;
            } else if (event.getCode() == KeyCode.A) {
                movingLeft = true;
            } else if (event.getCode() == KeyCode.D) {
                movingRight = true;
            } else if (event.getCode() == KeyCode.SPACE) {
                if (timeScale == 0) {//if the timescale is at 0 then unpause the game and get rid of the intro message
                    timeScale = 1.5;
                    UIContext.setVisible(false);
                }
                movingUp = true;
            } else if (event.getCode() == KeyCode.CONTROL) {
                movingDown = true;
            }
        });

        scene.setOnKeyReleased((KeyEvent event) -> {
            if (event.getCode() == KeyCode.W) {
                movingForward = false;
            } else if (event.getCode() == KeyCode.S) {
                movingBackward = false;
            } else if (event.getCode() == KeyCode.A) {
                movingLeft = false;
            } else if (event.getCode() == KeyCode.D) {
                movingRight = false;
            } else if (event.getCode() == KeyCode.SPACE) {
                movingUp = false;
            } else if (event.getCode() == KeyCode.CONTROL) {
                movingDown = false;
            }
        });

        scene.setOnScroll(event -> {
            double deltaY = event.getDeltaY(); // get the scroll distance
            cameraDistance -= deltaY; //change the camera distance based on scroll distance
            if (cameraDistance < minCameraDistance) { //keep the camera within certain bounds
                cameraDistance = minCameraDistance;
            }
            if (cameraDistance > maxCameraDistance) {
                cameraDistance = maxCameraDistance;
            }
        });

        AnimationTimer animationTimer = new AnimationTimer() {//this is used to make JavaFX 3d games run in real time
             /**
             * A method which causes the code within to execute 60 times a second
             * @param now the timestamp of the current frame
             */
            @Override
            public void handle(long now) {
                frameCounter++;//increment the frame counter and reset it when it's above 600
                if (frameCounter > 180) {
                    frameCounter = 0;
                }
                //camera controls
                //the camera target  follows the center of the player amoeba and applies dampening to make the camera motion more smooth
                cameraTargetVelocity = cameraTarget.subtract(amoebas[0].getTargetCenter()).multiply(0.1);
                cameraTargetVelocity = cameraTargetVelocity.multiply(0.9);
                cameraTarget = cameraTarget.subtract(cameraTargetVelocity.multiply(timeScale));

                //difference in mouse position is calculated
                double deltaMouseX = mouseX - previousMouseX;//get the difference between where the mouse is and where it was
                double deltaMouseY = mouseY - previousMouseY;
                previousMouseX = mouseX; //reset the previous mouse position to current mouse position
                previousMouseY = mouseY;
                mouseXMovement += deltaMouseX * cameraSpeed; //multiply the mouse movement by a scalar
                mouseYMovement += deltaMouseY * cameraSpeed;

                // Calculate new camera position, use  trigonometry to calculate its new position in a sphere around the center then add the position of the player amoeba
                cameraX = (cameraDistance * Math.sin(Math.toRadians(mouseXMovement)) * Math.cos(Math.toRadians(mouseYMovement))) + cameraTarget.getX();
                cameraY = (cameraDistance * Math.sin(Math.toRadians(mouseYMovement))) + cameraTarget.getY();
                cameraZ = (cameraDistance * Math.cos(Math.toRadians(mouseXMovement)) * Math.cos(Math.toRadians(mouseYMovement))) + cameraTarget.getZ();

                //ensure the camera doesn't go through the floor
                if (cameraY > -5) {
                    cameraY = -5;
                }

                //create a translation that will move the camera to the new position
                Translate moveCamera = new Translate(cameraX, cameraY, cameraZ);

                // calculate the angle from the camera to the player amoeba, first get the camera and amoeba position and assign them to temporary variables(to make it shorter and more readable), then calculate their difference
                Point3D from = new Point3D(cameraX, cameraY, cameraZ);
                Point3D to = cameraTarget;
                Point3D difference = to.subtract(from).normalize();

                //calculate the rotation of the camera along the x and y-axis by first getting the arc sin of the -y value of difference in radians then convert it to degrees
                //do the same thing with the arc tangent of the x and z of difference, then convert it to degrees
                double xRotation = Math.toDegrees(Math.asin(-difference.getY()));
                double yRotation = Math.toDegrees(Math.atan2(difference.getX(), difference.getZ()));

                //create two new rotates with those values, use the difference position as the pivot point, and the x-axis and y-axis respectively as the rotational axis for the two rotations
                Rotate rx = new Rotate(xRotation, difference.getX(), difference.getY(), difference.getZ(), Rotate.X_AXIS);
                Rotate ry = new Rotate(yRotation, difference.getX(), difference.getY(), difference.getZ(), Rotate.Y_AXIS);

                //clear all previous position/rotation transformations and apply new ones
                cameraAndUI.getTransforms().clear();
                cameraAndUI.getTransforms().addAll(moveCamera, ry, rx);

                if (movingForward) {
                    amoebas[0].addPlayerMovement(difference.multiply(2)); //add the difference between the camera position and amoeba position
                }
                if (movingBackward) {
                    amoebas[0].addPlayerMovement(difference.multiply(-2)); //subtract the difference between the camera position and amoeba position
                }
                if (movingLeft) {
                    amoebas[0].addPlayerMovement(difference.crossProduct(Rotate.Y_AXIS).normalize().multiply(2));// add the cross product of the difference and Y axis to velocity. Cross product returns the vector perpendicular to two others
                }
                if (movingRight) {
                    amoebas[0].addPlayerMovement(difference.crossProduct(Rotate.Y_AXIS).normalize().multiply(-2));// subtract the cross product of the difference and Y axis to velocity
                }
                if (movingUp) {
                    amoebas[0].addPlayerMovement(new Point3D(0, -2.5, 0));// add to the y velocity, a bit higher to counteract gravity
                }
                if (movingDown) {
                    amoebas[0].addPlayerMovement(new Point3D(0, 1.5, 0));// subtract from the y velocity
                }
                //one of the most important lines, this accesses the model and updates the amoeba positions then the positions of all vertices
                for (Amoeba amoeba : amoebas) {
                    amoeba.updateAmoebaPosition();
                }
            }
        };

        // Start the animation timer
        animationTimer.start();
        scene.setCamera(camera);

        // Show the stage

        primaryStage.show();
    }

    /**
     * display the message stating player won
     */
    public static void displayWinMessage() {
        AmoebaWars.UIContext.setVisible(true);
        AmoebaWars.timeScale = 0.01;
        UIContext.setMaterial(UIWinTexture);
    }
    /**
     * display the message stating player lost
     */
    public static void displayLoseMessage() {
        AmoebaWars.UIContext.setVisible(true);
        AmoebaWars.timeScale = 0.03;
        UIContext.setMaterial(UILoseTexture);
    }
    /**
     * main method
     * args:
     */
    public static void main(String[] args) {
        launch(args);
    }
}