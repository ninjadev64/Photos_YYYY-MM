package com.amansprojects.photos_yyyy_mm;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Route("authorise")
public class Authorise extends VerticalLayout {
    private static boolean authorised = false;
    public String authorisationUrl;

    public class AuthorisationURLDisplay implements AuthorizationCodeInstalledApp.Browser {
        @Override
        public void browse(String url) {
            authorisationUrl = url;
        }
    }

    public Authorise() throws InterruptedException {
        if (authorised) {
            UI.getCurrent().navigate(Main.class);
            return;
        }
        new Thread(() -> {
            try {
                Main.client = PhotosLibraryClientFactory.createClient(
                    new AuthorisationURLDisplay(),
                    List.of("https://www.googleapis.com/auth/photoslibrary.readonly")
                );
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }).start();
        while (authorisationUrl == null) {
            Thread.sleep(100);
        }
        authorised = true;
        UI.getCurrent().getPage().setLocation(authorisationUrl);
    }
}
