package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


public class HomePageController {

    static Stage primaryStage;

    @FXML private ImageView enterExitFullScreenIcon;
    //The enter/exit fullScreen Icons
    private final Image exitFullScreenIcon = new Image("sample/Icones/exitFullScreenIcon.png");
    private final Image enterFullScreenIcon = new Image("sample/Icones/fullScreenIcon.png");


    public void showDialog(ActionEvent actionEvent) throws Exception{
        System.out.println("Please Choose A Video File");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open a video file : ");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(".MP4", "*.mp4"));
        File selectedVideo = fileChooser.showOpenDialog(primaryStage);
        if( selectedVideo == null){
            //System.out.println("Please Choose a File!");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText("Please choose a File");
            alert.showAndWait();

        }
        else {
            String videoPath = selectedVideo.getAbsolutePath();
            System.out.println("Video Loaded!");
            nextPage();
            videoController.setSource(videoPath);
        }
    }

    public void openCamera(ActionEvent actionEvent) throws Exception{
           nextPage();
           videoController.setSource(""); //Empty string here signifies that the source we will be using is Camera(just for simplicity)
    }

    /**
     * Moves to the next page
     * @throws Exception
     */
    private void nextPage() throws Exception{
        Main.loader = new FXMLLoader(getClass().getResource("videoView.fxml"));
        Parent root = (Parent)Main.loader.load();
        primaryStage.getScene().setRoot(root);
    }

    public void enterFullScreen() {
        primaryStage.setFullScreenExitHint(""); //disables the fullscreen hint shown when opening in fullscreen mode
        if(!primaryStage.isFullScreen()){
            enterExitFullScreenIcon.setImage(exitFullScreenIcon);
            primaryStage.setFullScreen(true);
        } else {
            enterExitFullScreenIcon.setImage(enterFullScreenIcon);
            primaryStage.setFullScreen(false);
        }
    }

    public void exit() {

        javafx.application.Platform.exit();
    }
}
