package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        int opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar
                    4 - Buscar série por título
                    5 - Buscar séries por ator e avaliação
                    6 - Listar o top 5 series
                    7 - Buscar séries por Categoria
                    8 - Buscar series por quantidade de temporadas e avaliação
                                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarEpisodioPorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    listarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorAtorAvaliacao();
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }



    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        //dadosSeries.add(dados);

        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Escolha o nome da série pelo nome: \n");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha o nome da série pelo nome: \n");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serieFiltrada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieFiltrada.isPresent()) {
            var serieEncontrada = serieFiltrada.get();
            List<DadosTemporada> temporadas = new ArrayList<>();


            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(),e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios((episodios));
            repositorio.save(serieEncontrada);
        } else{
            System.out.println("Série não encontrada!!");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);

    }


    private void buscarEpisodioPorTitulo() {
        System.out.println("Digite o nome da série: \n");
        String nomeSerie = leitura.nextLine();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()){
            System.out.println("Série buscada:\n" + serieBuscada.get());
        }else{
            System.out.println("Série não encontrada: " + nomeSerie);
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Digite o nome do ator: \n");
        String nomeAtor = leitura.nextLine();
        System.out.println("Digite a avaliação que deseja filtrar: ");
        double avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor,avaliacao);

        System.out.println(seriesEncontradas);

    }
    private void listarTop5Series() {
        List<Serie> top5Series = repositorio.findTop5ByOrderByAvaliacaoDesc();
        top5Series.forEach(s -> System.out.println(s.getTitulo() + "Avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar séries por qual categoria/genero?: \n");
        String nomeCategoria = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeCategoria);
        List<Serie> seriesCategoria = repositorio.findByGenero(categoria);
        System.out.println("Séries da categoria: " + nomeCategoria);
        seriesCategoria.forEach(System.out::println);
    }

    private void buscarSeriesPorAtorAvaliacao() {
        System.out.println("Digite a quantidade de temporádas que deseja buscar: ");
        var quantidadeTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Digite a avaliação: ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> seriesEncontradas = repositorio.seriesPorTemporadaEAvaliacao(quantidadeTemporadas,avaliacao);
        seriesEncontradas.forEach(System.out::println);

    }
}