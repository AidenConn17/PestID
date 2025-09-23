package com.example.pestid;

import java.io.IOException;
import java.util.concurrent.Executor;

public class IdentificationViewModel {
    Executor executor;
    Identification identification = new Identification();
    public IdentificationViewModel(Executor executor) {
        this.executor = executor;
    }

    public void getInfoAboutInsect(String imageBase64) throws IOException {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    identification.getInfoAboutInsect(imageBase64);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
