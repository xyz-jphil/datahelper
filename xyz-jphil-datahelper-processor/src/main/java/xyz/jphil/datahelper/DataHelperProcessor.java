package xyz.jphil.datahelper;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation processor that generates {ClassName}_I interface with field symbols and fluent methods.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("xyz.jphil.datahelper.DataHelper")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DataHelperProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "Processing @DataHelper on " + element);
                    generateInterface((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void generateInterface(TypeElement element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).toString();
        String className = element.getSimpleName().toString();
        String interfaceName = className + "_I";

        // Extract property annotations and super interfaces from the @DataHelper annotation
        List<ClassName> propertyAnnotations = extractPropertyAnnotations(element);
        List<TypeName> superInterfaces = extractSuperInterfaces(element);

        // Collect all non-static, non-final fields
        List<FieldInfo> fields = new ArrayList<>();
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                Set<Modifier> modifiers = enclosedElement.getModifiers();
                if (!modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.FINAL)) {
                    VariableElement field = (VariableElement) enclosedElement;
                    String fieldName = field.getSimpleName().toString();
                    TypeName fieldType = TypeName.get(field.asType());
                    fields.add(new FieldInfo(fieldName, fieldType));
                }
            }
        }

        // Build the interface
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("E",
                        ParameterizedTypeName.get(
                                ClassName.get(packageName, interfaceName),
                                TypeVariableName.get("E")
                        )));

        // Always extend DataHelper_I<E> as the base contract
        interfaceBuilder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                TypeVariableName.get("E")
        ));

        // Add super interfaces if specified
        for (TypeName superInterface : superInterfaces) {
            interfaceBuilder.addSuperinterface(superInterface);
        }

        // Add field symbols
        for (FieldInfo field : fields) {
            String symbolName = "$" + field.name;
            FieldSpec symbol = FieldSpec.builder(String.class, symbolName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", field.name)
                    .build();
            interfaceBuilder.addField(symbol);
        }

        // Add FIELDS list
        CodeBlock.Builder fieldsListInitBuilder = CodeBlock.builder().add("$T.of(", List.class);
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                fieldsListInitBuilder.add(", ");
            }
            fieldsListInitBuilder.add("$N", "$" + fields.get(i).name);
        }
        fieldsListInitBuilder.add(")");

        FieldSpec fieldsListField = FieldSpec.builder(
                ParameterizedTypeName.get(List.class, String.class),
                "FIELDS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(fieldsListInitBuilder.build())
                .build();
        interfaceBuilder.addField(fieldsListField);

        // Add verbose getter declarations
        for (FieldInfo field : fields) {
            String getterName = "get" + capitalize(field.name);
            MethodSpec.Builder getterBuilder = MethodSpec.methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(field.type);

            // Add property annotations to verbose getter
            for (ClassName annotation : propertyAnnotations) {
                getterBuilder.addAnnotation(annotation);
            }

            interfaceBuilder.addMethod(getterBuilder.build());
        }

        // Add verbose setter declarations
        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            MethodSpec.Builder setterBuilder = MethodSpec.methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(field.type, field.name);

            // Add property annotations to verbose setter
            for (ClassName annotation : propertyAnnotations) {
                setterBuilder.addAnnotation(annotation);
            }

            interfaceBuilder.addMethod(setterBuilder.build());
        }

        // Add fluent getter defaults
        for (FieldInfo field : fields) {
            String getterName = "get" + capitalize(field.name);
            MethodSpec fluentGetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(field.type)
                    .addStatement("return $N()", getterName)
                    .build();
            interfaceBuilder.addMethod(fluentGetter);
        }

        // Add fluent setter defaults
        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            MethodSpec fluentSetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .addParameter(field.type, field.name)
                    .returns(TypeVariableName.get("E"))
                    .addStatement("$N($N)", setterName, field.name)
                    .addStatement("return (E) this")
                    .build();
            interfaceBuilder.addMethod(fluentSetter);
        }

        // Add toMap() method
        MethodSpec.Builder toMapBuilder = MethodSpec.methodBuilder("toMap")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ClassName.get(Object.class)))
                .addStatement("var entries = new $T<$T<$T, $T>>()",
                        ArrayList.class, Map.Entry.class, String.class, Object.class);

        for (FieldInfo field : fields) {
            String getterName = "get" + capitalize(field.name);
            String symbolName = "$" + field.name;

            // For primitive types, skip null check; for objects, check if not null
            if (field.type.isPrimitive()) {
                toMapBuilder.addStatement("entries.add($T.entry($N, $N()))",
                        Map.class, symbolName, getterName);
            } else {
                toMapBuilder.addStatement("if ($N() != null) entries.add($T.entry($N, $N()))",
                        getterName, Map.class, symbolName, getterName);
            }
        }

        toMapBuilder.addStatement("return $T.ofEntries(entries.toArray($T[]::new))", Map.class, Map.Entry.class);
        interfaceBuilder.addMethod(toMapBuilder.build());

        // Add fromValueProvider(Function<String, Object> valueProvider) method
        MethodSpec.Builder fromValueProviderBuilder = MethodSpec.methodBuilder("fromValueProvider")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addParameter(ParameterizedTypeName.get(
                        ClassName.get("java.util.function", "Function"),
                        ClassName.get(String.class),
                        ClassName.get(Object.class)), "valueProvider")
                .returns(TypeVariableName.get("E"));

        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            String symbolName = "$" + field.name;
            String varName = field.name + "Val";
            fromValueProviderBuilder.addStatement("var $N = valueProvider.apply($N)", varName, symbolName);
            fromValueProviderBuilder.addStatement("if ($N != null) $N(($T) $N)",
                    varName, setterName, field.type, varName);
        }

        fromValueProviderBuilder.addStatement("return (E) this");
        interfaceBuilder.addMethod(fromValueProviderBuilder.build());

        // Add fromTypedValueProvider(BiFunction<String, Class<?>, Object> typedValueProvider) method
        // This provides field name AND type information, useful for TeaVM JSValueConverter
        MethodSpec.Builder fromTypedValueProviderBuilder = MethodSpec.methodBuilder("fromTypedValueProvider")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addParameter(ParameterizedTypeName.get(
                        ClassName.get("java.util.function", "BiFunction"),
                        ClassName.get(String.class),
                        ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)),
                        ClassName.get(Object.class)), "typedValueProvider")
                .returns(TypeVariableName.get("E"));

        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            String symbolName = "$" + field.name;
            String varName = field.name + "Val";

            // Get the type class - for primitives use wrapper class
            TypeName typeForClass = field.type;
            if (field.type.equals(TypeName.INT)) {
                typeForClass = TypeName.get(Integer.class);
            } else if (field.type.equals(TypeName.LONG)) {
                typeForClass = TypeName.get(Long.class);
            } else if (field.type.equals(TypeName.DOUBLE)) {
                typeForClass = TypeName.get(Double.class);
            } else if (field.type.equals(TypeName.FLOAT)) {
                typeForClass = TypeName.get(Float.class);
            } else if (field.type.equals(TypeName.BOOLEAN)) {
                typeForClass = TypeName.get(Boolean.class);
            } else if (field.type.equals(TypeName.BYTE)) {
                typeForClass = TypeName.get(Byte.class);
            } else if (field.type.equals(TypeName.SHORT)) {
                typeForClass = TypeName.get(Short.class);
            } else if (field.type.equals(TypeName.CHAR)) {
                typeForClass = TypeName.get(Character.class);
            }

            // For parameterized types (List<String>, etc.), use raw type for .class
            if (typeForClass instanceof ParameterizedTypeName) {
                ParameterizedTypeName paramType = (ParameterizedTypeName) typeForClass;
                fromTypedValueProviderBuilder.addStatement("var $N = typedValueProvider.apply($N, $T.class)",
                        varName, symbolName, paramType.rawType);
            } else {
                fromTypedValueProviderBuilder.addStatement("var $N = typedValueProvider.apply($N, $T.class)",
                        varName, symbolName, typeForClass);
            }
            fromTypedValueProviderBuilder.addStatement("if ($N != null) $N(($T) $N)",
                    varName, setterName, field.type, varName);
        }

        fromTypedValueProviderBuilder.addStatement("return (E) this");
        interfaceBuilder.addMethod(fromTypedValueProviderBuilder.build());

        // Note: fromMap() is now provided by DataHelper_I base interface

        // Build and write the file
        TypeSpec interfaceSpec = interfaceBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, interfaceSpec)
                .indent("    ")
                .skipJavaLangImports(true)
                .addFileComment("Generated by DataHelperProcessor on " + LocalDateTime.now())
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Generated interface: " + interfaceName + " for class: " + className);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate interface: " + e.getMessage());
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Extracts the propertyAnnotations parameter from the @DataHelper annotation.
     * Returns a list of ClassName objects representing the annotation classes to apply.
     */
    private List<ClassName> extractPropertyAnnotations(TypeElement element) {
        List<ClassName> result = new ArrayList<>();

        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            // Check if this is the @DataHelper annotation
            if (annotationMirror.getAnnotationType().toString()
                    .equals("xyz.jphil.datahelper.DataHelper")) {

                // Get the annotation values
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
                        annotationMirror.getElementValues();

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("propertyAnnotations")) {
                        // The value is a list of annotation classes
                        @SuppressWarnings("unchecked")
                        List<? extends AnnotationValue> annotationClasses =
                                (List<? extends AnnotationValue>) entry.getValue().getValue();

                        for (AnnotationValue annotationClass : annotationClasses) {
                            DeclaredType annotationType = (DeclaredType) annotationClass.getValue();
                            TypeElement annotationElement = (TypeElement) annotationType.asElement();
                            String packageName = processingEnv.getElementUtils()
                                    .getPackageOf(annotationElement).toString();
                            String simpleName = annotationElement.getSimpleName().toString();
                            result.add(ClassName.get(packageName, simpleName));

                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    "Adding property annotation: " + packageName + "." + simpleName);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Extracts the superInterfaces parameter from the @DataHelper annotation.
     * Returns a list of TypeName objects representing the interfaces to extend.
     */
    private List<TypeName> extractSuperInterfaces(TypeElement element) {
        List<TypeName> result = new ArrayList<>();

        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            // Check if this is the @DataHelper annotation
            if (annotationMirror.getAnnotationType().toString()
                    .equals("xyz.jphil.datahelper.DataHelper")) {

                // Get the annotation values
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
                        annotationMirror.getElementValues();

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("superInterfaces")) {
                        // The value is a list of interface classes
                        @SuppressWarnings("unchecked")
                        List<? extends AnnotationValue> interfaceClasses =
                                (List<? extends AnnotationValue>) entry.getValue().getValue();

                        for (AnnotationValue interfaceClass : interfaceClasses) {
                            DeclaredType interfaceType = (DeclaredType) interfaceClass.getValue();
                            TypeElement interfaceElement = (TypeElement) interfaceType.asElement();
                            String packageName = processingEnv.getElementUtils()
                                    .getPackageOf(interfaceElement).toString();
                            String simpleName = interfaceElement.getSimpleName().toString();
                            result.add(ClassName.get(packageName, simpleName));

                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    "Adding super interface: " + packageName + "." + simpleName);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static class FieldInfo {
        final String name;
        final TypeName type;

        FieldInfo(String name, TypeName type) {
            this.name = name;
            this.type = type;
        }
    }
}
