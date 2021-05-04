package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.*;
import org.opencv.videoio.VideoCapture;

import org.opencv.videoio.Videoio;
import utils.Utils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

public class videoController {

    static Stage primaryStage;
    static Parent mainRoot; //home page
    static String videoPath;

    // the Screen dimensions
    private final double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
    private final double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();

    // ---------------- Controls : -----------------------
    @FXML private ImageView videoContainer;
    @FXML private Button playBtn;
    @FXML private ImageView playPauseIcon;
    @FXML private ImageView enterExitFullScreenIcon;
    @FXML private ToggleGroup objectClasses;

    //The play & pause Icons
    private final Image playIcon = new Image("sample/Icones/playIcon.png");
    private final Image pauseIcon = new Image("sample/Icones/pauseIcon.png");
    //The enter/exit fullScreen Icons
    private final Image exitFullScreenIcon = new Image("sample/Icones/exitFullScreenIcon.png");
    private final Image enterFullScreenIcon = new Image("sample/Icones/fullScreenIcon.png");

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    // the id of the camera to be used
    private final static int cameraId = 0;
    //------------- Flags : -----------------
    // a flag to change the button behavior
    private boolean isCameraActive;
    //flag for the stage dimensions
    private boolean isStageSet;

    // Video resolution
    private double frameWidth;
    private double frameHeight;

    //A variable where we will store the ratio of the video with a default value of  1:1
    private double ratio = 1.0;

    //Object to detect index
    private int objectToDetect;


    /**
     * Where the actuall work happen,
     * @param event
     */
    @FXML
    protected void start(ActionEvent event) {

        if (!isCameraActive) {
            // start the video capture
            if(videoController.videoPath.isEmpty())
                capture.open(cameraId, Videoio.CAP_DSHOW); // Open Camera (force it to use CAP_DSHOW API backends(this solves the async termination callback warning))
            else
                capture.open(videoController.videoPath); //Open video stream

            // is the video stream available?
            if (capture.isOpened()) {
                isCameraActive = true;
                 //Loading the Yolo model
                Net net = loadModel();
                // Classes for the possible object
                String[] names = {"Person","Bicycle","Car","Motorbike","Aeroplane","Bus","train","truck","boat","traffic light","fire hydrant","stop sign","parking meter","bench","bird","Cat","Dog","horse","sheep","cow","elephant","bear","zebra","giraffe","backpack","umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite","baseball bat","baseball glove","skateboard","tennis racket","bottle","wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","sofa","potted plant","bed","dining table","toilet","tv monitor","laptop","mouse","remote","keyboard","cell phone","microwave","oven","toaster","sink","refrigerator","book","clock","vase","scissors","teddy bear","hair drier","toothbrush"};
               //Setting up the objectToDetect field
                RadioMenuItem selectedClass = (RadioMenuItem) objectClasses.getSelectedToggle();
                String classID = selectedClass.getId();
                objectToDetect = classID.equals("all") ? -1 : parseInt(classID);

                // Start detecting
                System.out.println("Detecting...");
                List<Mat> result = new ArrayList<>();
                List<String> outBlobNames = getOutputNames(net);
                float confThreshold = 0.5f;
                // grab a frame every 33 ms
                Runnable frameGrabber = () -> {
                    //long t0 = System.nanoTime();
                    // effectively grab and process a single frame
                    Mat frame = grabFrame();

                    //Updating the stage dimensions basing on the frame's
                    if(!isStageSet){
                        frameWidth = frame.width();
                        frameHeight = frame.height();
                        adjustStage(frameWidth, frameHeight);
                        isStageSet = true;
                    }

                    //------------------ Start detecting here : --------------------------------
                    Mat blob = Dnn.blobFromImage(frame, 0.00392, new Size(416, 416), new Scalar(0), true, false); // We feed one frame of video into the network at a time, we have to convert the image to a blob. A blob is a pre-processed image that serves as the input.//
                    net.setInput(blob);
                    net.forward(result, outBlobNames); // Feed forward the model to get output

                    List<Integer> classesIDs = new ArrayList<>(); // stores the classes' ids of the detected objects
                    List<Float> probabilities = new ArrayList<>(); // stores the classes' ids of the detected objects
                    List<Rect> boundingBoxes = new ArrayList<>();

                    for (Mat level : result) {
                        // each row is a candidate detection, the 1st 4 numbers are
                        // [centerX, centerY, width, height], followed by (N-4) class probabilities
                        for (int j = 0; j < level.rows(); ++j) {
                            Mat row = level.row(j);
                            Mat scores = row.colRange(5, level.cols());
                            Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                            float confidence = (float) mm.maxVal;
                            Point classIdPoint = mm.maxLoc;

                            //Only store the objects detected with a confidence over the specified threshold(i.e > 70%)
                            if (confidence > confThreshold) {
                                int centerX = (int) (row.get(0, 0)[0] * frame.cols()); // scaling for drawing the bounding boxes
                                int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                                int width = (int) (row.get(0, 2)[0] * frame.cols());
                                int height = (int) (row.get(0, 3)[0] * frame.rows());
                                int left = centerX - width / 2;
                                int top = centerY - height / 2;

                                classesIDs.add((int) classIdPoint.x);
                                probabilities.add(confidence);
                                //System.out.println(names[(int) classIdPoint.x] + " Detected!");
                                boundingBoxes.add(new Rect(left, top, width, height));
                            }
                        }
                    }

                    if(!probabilities.isEmpty())
                    {
                        MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(probabilities));
                        Rect[] boxesArray = boundingBoxes.toArray(new Rect[0]);
                        MatOfRect boxes = new MatOfRect(boxesArray);
                        MatOfInt indices = new MatOfInt(); // where we will store the indices(IDs) of the detected objects after NMS
                        // Applying non-maximum suppression on the detected objects
                        //this eliminates the overlapped boxes
                        Dnn.NMSBoxes(boxes, confidences, confThreshold, 0.2f, indices);

                        // Drawing the bounding boxes for the detected objects
                        int[] detectedObjects = indices.toArray(); //Storing the detected objects' ids
                        // Displaying the bounding boxes
                        for (int index : detectedObjects) {
                            // filtering detected objects
                            if (classesIDs.get(index) == objectToDetect || objectToDetect == -1) {
                            // One of the two conditions will be true at a time
                            //objectToDetect == -1 will be true only when the "All" is chosen in the menu
                                Rect box = boxesArray[index];
                                Random rand = new Random();
                                int max = 256;
                                int redVal = rand.nextInt(max);
                                int greenVal = rand.nextInt(max);
                                int blueVal = rand.nextInt(max);
                                Scalar boxColor = new Scalar(redVal, greenVal, blueVal);
                                // Drawing the bounding box
                                Imgproc.rectangle(frame, box, boxColor, 2);
                                //----------- drawing the label :----------------
                                // Formating the label
                                float accuracy = probabilities.get(index) * 100;
                                int classId = classesIDs.get(index);
                                String label = names[classId] + " : " + (int) accuracy + "%";
                                //drawing the label's background
                                int[] baseline = {0};
                                Size labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 0.4, 2, baseline);
                                double ox = box.tl().x;
                                double oy = box.tl().y;
                                double labelWidth = labelSize.width;
                                double labelHeight = labelSize.height;
                                Imgproc.rectangle(frame, new Point(ox - 1, oy),new Point(ox + 2 + labelWidth,oy - 4 - labelHeight), boxColor, Imgproc.FILLED,0,0);
                                // Drawing the text
                                Imgproc.putText(frame, label, new Point(ox + 2, oy - 3), Imgproc.FONT_HERSHEY_SIMPLEX, 0.4, new Scalar(0, 0, 0));
                            }
                        }
                    }

                    // Convert and show the frame
                    Image imageToShow = Utils.mat2Image(frame);
                    updateImageView(videoContainer, imageToShow);
                    //long t1 = System.nanoTime();
                    //System.out.println("took : " + ((t1 - t0)*Math.pow(10, -9)) + " seconds");
                };

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // Update the button content
                playBtn.setText("Stop");
                playPauseIcon.setImage(pauseIcon);
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to open the video stream!");
                alert.showAndWait();
                //System.err.println("Failed to open the video stream!");
            }

        } else {
            // The camera is not active
            isCameraActive = false;
            // Update the button content
            playBtn.setText("Play");
            playPauseIcon.setImage(playIcon);

            // Release resources
            releaseResources();
        }
    }

    //--------------------------------------------- loadModel(): -----------------------------------------------//
    /**
     * Loads the Yolo model(i.e configuration file and weights)
     * @return
     */
    private Net loadModel() {

        System.out.println("Loading Model...");
        String modelWeights = "src/darknet/weights/yolov3-tiny.weights";
        String modelConfiguration = "src/darknet/cfg/yolov3-tiny.cfg.txt";
        Net net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);
        System.out.println("Model Loaded!");

        return net;
    }

    //--------------------------------------------- getOutputNames(): -----------------------------------------------//
    private static List<String> getOutputNames(Net net) {
        List<String> names = new ArrayList<>();

        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String>  layersNames = net.getLayerNames();
        // Unfold and create R-CNN layers from the loaded YOLO model
        outLayers.forEach((item) -> names.add(layersNames.get(item - 1)));
        return names;
    }

    //--------------------------------------------- grabFrame(): -----------------------------------------------//
    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
    private Mat grabFrame() {
        // a Mat variable which will hold the frame
        Mat frame = new Mat();

        // check if the capture is open
        if (capture.isOpened()) {
            try {
                // read the current frame
                capture.read(frame);
            }
            catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Couldn't read the frame! ");
                alert.showAndWait();
                //System.err.println("Couldn't read the frame! ");
            }
        }
        return frame;
    }

    //--------------------------------------------- releaseResources(): -----------------------------------------------//
    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void releaseResources() {
        if (timer != null && !timer.isShutdown()) {
            try {
                // stop the timer
                timer.shutdown();
                timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                // log any exception
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Couldn't stop the camera!");
                alert.showAndWait();
                //System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }
        //Release the video stream if it's opened
        if (capture.isOpened()) {
            // release the camera
            System.out.println("Releasing Camera...");
            capture.release();
            System.out.println("Camera Released!");
        }
    }

    //--------------------------------------------- setSource(): -----------------------------------------------//
    /**
     * Setting the source from which the frames will be taken; empty string indicates Camera non empty String
     * indicates video
     * @param videoPath
     */
    public static void setSource(String videoPath){

        videoController.videoPath = videoPath;
    }

    // ------------------------------------------------------------------- View : ------------------------------------------------------------------------//

    //--------------------------------------------- adjustStage(): -----------------------------------------------//
    /**
     * Adjusts the stage's dimensions
     * @param frameWidth
     * @param frameHeight
     */
    private void adjustStage(double frameWidth, double frameHeight) {
        ratio = frameWidth/frameHeight; //calculation the aspect ratio of the video
        //When the video can't fit the screen resize it preserving the ratio
        if( (frameWidth >= screenWidth || frameHeight >= screenHeight) && !primaryStage.isFullScreen() ){
            frameHeight = 500; // default height
            frameWidth = frameHeight * ratio; //preserving the ratio
            videoContainer.setFitHeight(frameHeight);
            videoContainer.setFitWidth(frameWidth);
            primaryStage.setWidth(frameWidth);
            primaryStage.setHeight(frameHeight + 26 + 36);
            centerStage();
        }
        else if(!primaryStage.isFullScreen()){
            primaryStage.setWidth(frameWidth);
            primaryStage.setHeight(frameHeight + 26 + 36);
            centerStage();
        } else {
            videoContainer.setFitHeight(primaryStage.getHeight() - (26 + 36));
            videoContainer.setFitWidth(videoContainer.getFitHeight() * ratio);
        }
    }

    /**
     * Centers the stage
     */
    private void centerStage() {
        //Centering the stage on the screen
        primaryStage.setX((screenWidth - primaryStage.getWidth())/2);
        primaryStage.setY((screenHeight - primaryStage.getHeight())/2);
    }

    //--------------------------------------------- updateImageView(): -----------------------------------------------//
    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view
     *            the {@link ImageView} to update
     * @param image
     *            the {@link javafx.scene.image.Image} to show
     */
    private void updateImageView(ImageView view, Image image) {

        Utils.onFXThread(view.imageProperty(), image);
    }

    //--------------------------------------------- goHome(): -----------------------------------------------//
    /**
     * Returning to the home page
     */
    public void goHome() {
        //Release any opened resources before going back to home page
        if(isCameraActive)
            releaseResources();
        //if the stage was adjusted, reset it
        if(isStageSet)
            adjustStage(720, 500);
        //change root to the main root (home page)
        primaryStage.getScene().setRoot(mainRoot);
    }

    //--------------------------------------------- enterFullScreen(): -----------------------------------------------//
    /**
     * This method handles the full screen mode (i.e entering and exiting and updating
     * the view accordingly
     */
    public void enterFullScreen() {
        primaryStage.setFullScreenExitHint(""); //disables the fullscreen hint shown when opening fullscreen mode
        if(!primaryStage.isFullScreen()){
            enterExitFullScreenIcon.setImage(exitFullScreenIcon);
            primaryStage.setFullScreen(true);

        } else {
            enterExitFullScreenIcon.setImage(enterFullScreenIcon);
            primaryStage.setFullScreen(false);
            // set to the initial resolution
            if(isStageSet){
                primaryStage.setHeight(500 + 26 + 36);
                primaryStage.setWidth(500*ratio);
                // Centering the stage
                centerStage();
            }

        }
        // when entering/exiting the full screen mode adjust the video accordingly
        // preserving the ratio
        videoContainer.setFitHeight(primaryStage.getHeight() - (26 + 36));
        videoContainer.setFitWidth(videoContainer.getFitHeight() * ratio);

    }

    //--------------------------------------------- exit(): -----------------------------------------------//
    /**
     * On application close, close any opened resources
     */
    public void exit() {
        System.out.println("Releasing resources...");
        releaseResources();
        javafx.application.Platform.exit();
        System.out.println("Resources released!");
    }
}
