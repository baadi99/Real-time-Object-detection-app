package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {

    static FXMLLoader loader;

    @Override
    public void start(Stage primaryStage) throws Exception{
        HomePageController.primaryStage = primaryStage;
        videoController.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.TRANSPARENT); // hiding the default stage(Window) style
        Parent root = FXMLLoader.load(getClass().getResource("homePageView.fxml"));
        videoController.mainRoot = root;
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void stop(){
        System.out.println("closing window...");
    }


    public static void main(String[] args) {
        // Getting the current working directory
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Loading Library...");
        System.load(currentDirectory + "/src/opencv/Build/Java/x64/opencv_java400.dll");
        System.out.println("Library Loaded!");
        launch(args);
    }


}
