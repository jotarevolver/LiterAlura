package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private String URL_BASE = "https://gutendex.com/books/";
    private AutorRepository repository;

    public Principal(AutorRepository repository) {
        this.repository = repository;
    }

    public void mostrarMenu() {
        Integer opcion = -1;
        var menu = """
                -----  Bienvenido a Literalura  -----
                --------------------------------------------
                              MENU PRINCIPAL
                --------------------------------------------
                1.- Buscar libros por titulo
                2.- Mostrar libros registrados
                3.- Mostrar autores registrados
                4.- Mostrar autores vivos
                5.- Mostrar libros por idioma
                ----------------------------------------------
                0.- Salir del programa
                ----------------------------------------------
                Ingrese una opción:
                """;

        while (opcion != 0) {
            System.out.println(menu);
            try {
                opcion = teclado.nextInt();
                teclado.nextLine();
                switch (opcion) {
                    case 1:
                        buscarLibroPorTitulo();
                        break;
                    case 2:
                        listarLibrosRegistrados();
                        break;
                    case 3:
                        listarAutoresRegistrados();
                        break;
                    case 4:
                        listarAutoresVivos();
                        break;
                    case 5:
                        listarLibrosPorIdioma();
                        break;

                    case 0:
                        System.out.println("Gracias por utilizar Literalura");
                        System.out.println("Cerrando la aplicacion Literalura ...");
                        break;
                    default:
                        System.out.println("Opción no válida!");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida: " + e.getMessage());

            }
        }
    }

    public void buscarLibroPorTitulo() {
        System.out.println("""
                --------------------------------
                  BUSCAR LIBROS POR TÍTULO
                --------------------------------
                 """);
        System.out.println("Introduzca el nombre del libro que desea buscar:");
        var nombre = teclado.nextLine();
        System.out.println("Cargando...");
        System.out.println("Espere por favor");
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());

        // Check if JSON is empty
        if (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]")) {
            var datos = conversor.obtenerDatos(json, Datos.class);

            // Process valid data
            Optional<DatosLibro> libroBuscado = datos.libros().stream()
                    .findFirst();
            if (libroBuscado.isPresent()) {
                System.out.println(
                        "\n------------- LIBRO --------------" +
                                "\nTítulo: " + libroBuscado.get().titulo() +
                                "\nAutor: " + libroBuscado.get().autores().stream()
                                        .map(a -> a.nombre()).limit(1).collect(Collectors.joining())
                                +
                                "\nIdioma: " + libroBuscado.get().idiomas().stream().collect(Collectors.joining()) +
                                "\nNúmero de descargas: " + libroBuscado.get().descargas() +
                                "\n--------------------------------------\n");

                try {
                    List<Libro> libroEncontrado = libroBuscado.stream().map(a -> new Libro(a))
                            .collect(Collectors.toList());
                    Autor autorAPI = libroBuscado.stream().flatMap(l -> l.autores().stream()
                            .map(a -> new Autor(a)))
                            .collect(Collectors.toList()).stream().findFirst().get();
                    Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                            .map(a -> a.nombre())
                            .collect(Collectors.joining()));
                    Optional<Libro> libroOptional = repository.buscarLibroPorNombre(nombre);
                    if (libroOptional.isPresent()) {
                        System.out.println("El libro ya está guardado en la BD.");
                    } else {
                        Autor autor;
                        if (autorBD.isPresent()) {
                            autor = autorBD.get();
                            System.out.println("EL autor ya esta guardado en la BD");
                        } else {
                            autor = autorAPI;
                            repository.save(autor);
                        }
                        autor.setLibros(libroEncontrado);
                        repository.save(autor);
                    }
                } catch (Exception e) {
                    System.out.println("Warning! " + e.getMessage());
                }
            } else {
                System.out.println("Libro no encontrado!");
            }
        }
    }

    public void listarLibrosRegistrados() {
        System.out.println("""
                ----------------------------------
                  LISTAR LIBROS REGISTRADOS
                ----------------------------------
                 """);
        List<Libro> libros = repository.buscarTodosLosLibros();
        libros.forEach(l -> System.out.println(
                "-------------- LIBRO -----------------" +
                        "\nTítulo: " + l.getTitulo() +
                        "\nAutor: " + l.getAutor().getNombre() +
                        "\nIdioma: " + l.getIdioma().getIdioma() +
                        "\nNúmero de descargas: " + l.getDescargas() +
                        "\n----------------------------------------\n"));
    }

    public void listarAutoresRegistrados() {
        System.out.println("""
                ----------------------------------
                  LISTAR AUTORES REGISTRADOS
                ----------------------------------
                 """);
        List<Autor> autores = repository.findAll();
        System.out.println();
        autores.forEach(l -> System.out.println(
                "Autor: " + l.getNombre() +
                        "\nFecha de Nacimiento: " + l.getNacimiento() +
                        "\nFecha de Fallecimiento: " + l.getFallecimiento() +
                        "\nLibros: " + l.getLibros().stream()
                                .map(t -> t.getTitulo()).collect(Collectors.toList())
                        + "\n"));
    }

    public void listarAutoresVivos() {
        System.out.println("""
                -----------------------------
                   LISTAR AUTORES VIVOS
                -----------------------------
                 """);
        System.out.println("Introduzca un año para verificar el autor(es) que desea buscar:");
        try {
            var fecha = Integer.valueOf(teclado.nextLine());
            List<Autor> autores = repository.buscarAutoresVivos(fecha);
            if (!autores.isEmpty()) {
                System.out.println();
                autores.forEach(a -> System.out.println(
                        "Autor: " + a.getNombre() +
                                "\nFecha de Nacimiento: " + a.getNacimiento() +
                                "\nFecha de Fallecimiento: " + a.getFallecimiento() +
                                "\nLibros: " + a.getLibros().stream()
                                        .map(l -> l.getTitulo()).collect(Collectors.toList())
                                + "\n"));
            } else {
                System.out.println("No hay autores vivos en el año registrado");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ingresa un año válido " + e.getMessage());
        }
    }

    public void listarLibrosPorIdioma() {
        System.out.println("""
                --------------------------------
                  LISTAR LIBROS POR IDIOMA
                --------------------------------
                """);
        var menu = """
                ---------------------------------------------------
                Seleccione el idioma del libro que desea encontrar:
                ---------------------------------------------------
                1 - Español
                2 - Francés
                3 - Inglés
                4 - Portugués
                ----------------------------------------------------
                """;
        System.out.println(menu);

        try {
            var opcion = Integer.parseInt(teclado.nextLine());

            switch (opcion) {
                case 1:
                    buscarLibrosPorIdioma("es");
                    break;
                case 2:
                    buscarLibrosPorIdioma("fr");
                    break;
                case 3:
                    buscarLibrosPorIdioma("en");
                    break;
                case 4:
                    buscarLibrosPorIdioma("pt");
                    break;
                default:
                    System.out.println("Opción inválida!");
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.buscarLibrosPorIdioma(idiomaEnum);
            if (libros.isEmpty()) {
                System.out.println("No hay libros registrados en ese idioma");
            } else {
                System.out.println();
                libros.forEach(l -> System.out.println(
                        "----------- LIBRO --------------" +
                                "\nTítulo: " + l.getTitulo() +
                                "\nAutor: " + l.getAutor().getNombre() +
                                "\nIdioma: " + l.getIdioma().getIdioma() +
                                "\nNúmero de descargas: " + l.getDescargas() +
                                "\n----------------------------------------\n"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido en el formato especificado.");
        }
    }

}
