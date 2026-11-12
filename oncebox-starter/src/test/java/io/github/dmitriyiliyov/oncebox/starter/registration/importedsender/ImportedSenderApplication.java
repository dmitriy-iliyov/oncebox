package io.github.dmitriyiliyov.oncebox.starter.registration.importedsender;

import io.github.dmitriyiliyov.oncebox.starter.registration.importedsenderconfig.ImportedSenderConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ImportedSenderConfiguration.class)
public class ImportedSenderApplication {
}
