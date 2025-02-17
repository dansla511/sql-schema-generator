package sk.tuke.meta.processor;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.persistence.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes({"javax.persistence.Table", "javax.persistence.Column"})
public class DatabaseManagerProcessor extends AbstractProcessor {

    private Writer w;

    private TemplateEngine templateEngine;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode("TEXT");
        templateResolver.setSuffix("");
        templateResolver.setPrefix("processor/src/main/resources/templates/");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        try {
            w = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "db.sql").openWriter();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> tables = roundEnv.getElementsAnnotatedWith(Table.class);

        if(tables.isEmpty()){
            try {
                w.close();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
            return true;
        }

        writeManager(tables);

        tables.forEach(this::writeDAO);

        tables.forEach(t -> writeTable(t, w));

        return true;
    }

    private void writeTable(Element table, Writer w){
        Table annotation = table.getAnnotation(Table.class);
        try {

            if(annotation.name().isEmpty()){
                w.write("CREATE TABLE IF NOT EXISTS " + table.getSimpleName() + "(\n");
            }
            else{
                w.write("CREATE TABLE IF NOT EXISTS " + annotation.name() + "(\n");
            }

            Iterator<? extends Element> fields = table.getEnclosedElements().stream().filter(e -> e.getAnnotation(Column.class) != null).iterator();

            while(fields.hasNext()){
                writeColumn(fields.next(), w);
                if(!fields.hasNext()){
                    w.write("\n");
                }
                else{
                    w.write(",\n");
                }
            }

            w.write(");\n\n");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private void writeColumn(Element column, Writer w){
        Column annotation = column.getAnnotation(Column.class);
        try {
            StringBuilder columnString = new StringBuilder("    ");

            if(annotation.name().isEmpty()){
                columnString.append(column.getSimpleName());
            }
            else{
                columnString.append(annotation.name());
            }

            switch (column.asType().toString()) {
                case "java.lang.String" -> columnString.append(" TEXT");
                case "float", "double" -> columnString.append(" REAL");
                default -> columnString.append(" INTEGER");
            }

            if(!annotation.nullable()){
                columnString.append(" NOT NULL");
            }

            if(annotation.unique()){
                columnString.append(" UNIQUE");
            }

            if(column.getAnnotation(Id.class) != null){
                columnString.append(" PRIMARY KEY AUTOINCREMENT");
            }

            if(column.getAnnotation(ManyToOne.class) != null){
                columnString.append(",\n");

                columnString.append("    FOREIGN KEY (");

                if(annotation.name().isEmpty()){
                    columnString.append(column.getSimpleName());
                }
                else{
                    columnString.append(annotation.name());
                }

                columnString.append(")\n");

                Element element;

                if(column.getAnnotation(ManyToOne.class).fetch() == FetchType.LAZY) {

                    //toto je cele strasne... fuj...
                    TypeMirror mirror = null;
                    try {
                        column.getAnnotation(ManyToOne.class).targetEntity();
                    } catch (MirroredTypeException e) {
                        mirror = e.getTypeMirror();
                    }

                    element = processingEnv.getTypeUtils().asElement(mirror);
                }
                else{
                    //z nejakeho dovodu musim urobit tento step aby som ziskal manytoone classu z ktorej
                    //mozem vytiahnut anotacie, pouzitie rovno column mi anotacie tej classy neziska
                    element = processingEnv.getTypeUtils().asElement(column.asType());
                }

                if(element.getAnnotation(Table.class).name().isEmpty()){
                    columnString.append("       REFERENCES ").append(column.asType().toString());
                }
                else{
                    columnString.append("       REFERENCES ").append(element.getAnnotation(Table.class).name());
                }

                Optional<? extends Element> idField = element.getEnclosedElements().stream().filter(
                        f -> f.getAnnotation(Id.class) != null).findFirst();

                columnString.append("(");

                if(idField.get().getAnnotation(Column.class).name().isEmpty()){
                    columnString.append(idField.get().getSimpleName());
                }
                else{
                    columnString.append(idField.get().getAnnotation(Column.class).name());
                }

                columnString.append(")");

                columnString.append("\n       ON DELETE SET NULL");


            }

            w.write(columnString.toString());

        } catch (IOException e){

            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private void writeDAO(Element table){

        Context ctx = new Context();
        Writer w = null;

        try {
            w = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT,
                    "", table.getSimpleName()+"DAO.java").openWriter();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }

        ctx.setVariable("type", table.getSimpleName());
        templateEngine.process("DAOTemplate", ctx, w);

        try {
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeManager(Set<? extends Element> tables){

        Object[] mappedTables = tables.stream().map(Element::getSimpleName).toArray();

        Context ctx = new Context();
        Writer w = null;

        try {
            w = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT,
                    "", "GeneratedPersistenceManager.java").openWriter();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }

        ctx.setVariable("entities", mappedTables);
        templateEngine.process("GeneratedManagerTemplate", ctx, w);

        try {
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
