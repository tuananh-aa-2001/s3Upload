package com.example.application.views.helloworld;

import com.example.application.ExoscaleS3Config;
import com.example.application.ExoscaleS3Service;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@PageTitle("Hello World")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class HelloWorldView extends VerticalLayout {

    private final MemoryBuffer buffer;
    private final Upload upload;
    private String objectKey;
    private String bucketName = "ieco-filestorage";


    @Autowired
    private ExoscaleS3Service exoscaleS3Service;

    public HelloWorldView()  {
        addClassName("centered-content");
        this.exoscaleS3Service = new ExoscaleS3Service();

        TextField link = new TextField("Link");
        link.setWidthFull();

        MultiSelectComboBox<String> bucketNameField = new MultiSelectComboBox<>("Bucket Name");


        link.addValueChangeListener(e -> {
            byte[] imageBytes = exoscaleS3Service.downloadResource(bucketName, objectKey);
            StreamResource streamResource = new StreamResource(objectKey,
                    () -> new ByteArrayInputStream(imageBytes));

            bucketNameField.setValue(exoscaleS3Service.listBucketLists());
            Notification.show("Die Länge des Buckets :  " + exoscaleS3Service.listBucketLists().size());
            Anchor downloadLink = new Anchor(streamResource, "Download");
            downloadLink.getElement().setAttribute("download", true);
            add(downloadLink);
        });

        Button deleteButton = new Button("Delete", event -> {
            exoscaleS3Service.deleteObject(bucketName, objectKey);
            Notification.show("Object deleted in " + bucketName + " with key " + objectKey);
        });


        Button listObjectsButton = new Button("List Objects", event ->listObjects());

        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        add(upload,bucketNameField,link,listObjectsButton,deleteButton);
        uploadFile(link);
    }

    private void listObjects() {
        boolean checkExist = exoscaleS3Service.doesObjectExistByListObjects(bucketName, objectKey);
        if(checkExist){
            List<String> objects = exoscaleS3Service.listObjects(bucketName);
            if (objects == null || objects.isEmpty()) {
                Notification.show("No objects found or the specified key does not exist.");
            } else {
                Grid<String> grid = new Grid<>(String.class);
                grid.setItems(objects);
                add(grid);
            }
        }else{
            Notification.show("No objects found or the specified key does not exist.");
        }
    }

    private void uploadFile(TextField link) {
        upload.addSucceededListener(event -> {
            try {
                InputStream is = buffer.getInputStream();
                File file= new File(event.getFileName());
                FileUtils.copyInputStreamToFile(is,file);

                objectKey = file.getName();
                exoscaleS3Service.uploadFile(bucketName, objectKey, file.getAbsolutePath());
                link.setValue(exoscaleS3Service.getUrl(bucketName, objectKey));
                Notification.show("File uploaded");
            }catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

}
