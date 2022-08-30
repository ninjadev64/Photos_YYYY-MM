package com.amansprojects.photos_yyyy_mm;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient.SearchMediaItemsPagedResponse;
import com.google.photos.library.v1.proto.DateFilter;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.types.proto.MediaItem;
import com.google.type.Date;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

@Route
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class Main extends VerticalLayout {
    public record Item(String url, boolean video, String filename) { }

    static PhotosLibraryClient client = null;

    public Main() {
        if (client == null) {
            HorizontalLayout authorise = new HorizontalLayout();
            authorise.add(new Text("Please authorise: "), new Anchor("/authorise", "here"));
            add(authorise);
            return;
        }

        IntegerField yearField = new IntegerField("Year");
        IntegerField monthField = new IntegerField("Month");

        Button submit = new Button("Submit");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(event -> {
            Integer year = yearField.getValue(), month = monthField.getValue();
            if (month != null) {
                Notification.show("Saved " + savePhotos(client, year, month) + " photos from " + year+"/"+String.format("%02d", month))
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                int count = 0;
                for (int m = 1; m <= 12; m++) count+=savePhotos(client, year, m);

                Notification.show("Saved " + count + " photos from " + year).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });

        add(yearField, monthField, submit);
    }

    public static int savePhotos(PhotosLibraryClient client, int year, int month) {
        Date date = Date.newBuilder().setYear(year).setMonth(month).build();
        SearchMediaItemsPagedResponse response = client.searchMediaItems(
            Filters.newBuilder().setDateFilter(DateFilter.newBuilder().addDates(date).build()).build()
        );

        int count = 0;
        ArrayList<Item> items = new ArrayList<Item>();
        for (MediaItem item : response.iterateAll()) {
            items.add(new Item(item.getBaseUrl(), item.getMediaMetadata().hasVideo(), item.getFilename()));
        }
        for (Item item : items) {
            try {
                String url = item.url + (item.video ? "=dv-no" : "=d");

                File directory = new File("Photos/" + year+"/"+year+"-"+String.format("%02d", month));
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        System.out.println("Failed to create directory");
                        return -1;
                    }
                }

                InputStream in = new URL(url).openStream();
                FileOutputStream fos = new FileOutputStream(
                    directory.getCanonicalPath() + "/" + item.filename
                );
                ReadableByteChannel rbc = Channels.newChannel(in);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }
}
