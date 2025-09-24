package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.EpisodioRepository;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSerie = new  ArrayList<>();

    private SerieRepository serieRepository;
    private EpisodioRepository episodioRepository;

    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository serieRepository, EpisodioRepository episodioRepository) {
        this.serieRepository = serieRepository;
        this.episodioRepository = episodioRepository;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0){
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar Séries Buscadas
                4 - Buscar Série por Titulo
                5 - Buscar Series por Ator
                6 - Buscar Top 5 Series Por Avaliação
                7 - Buscar Series por Categoria
                8 - Buscar Series por num de temporadas e por avaliação
                9 - Buscar Episodios por Trecho
                10 - Buscar Top 5 Episodios por Serie
                11 - Buscar Episodios a partir de uma data
                
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
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorNumTemporadasAvaliacao();
                    break;
                case 9:
                    buscarEpisodiosPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosAposData();
                    break;
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
        //dadosSerie.add(dados);
        serieRepository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> firstSerie = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);

        if(firstSerie.isPresent()){
            var serieEncontrada = firstSerie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodiosPorTemporada = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodiosPorTemporada);
            serieRepository.save(serieEncontrada);
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
        series = serieRepository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo titulo: ");
        var nomeSerie = leitura.nextLine();
        serieBuscada = serieRepository.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBuscada.isPresent()){
            System.out.println("Dados da serie: " + serieBuscada.get());
        } else {
            System.out.println("Serie não encontrada!");
        }
    }

    private void buscarSeriePorAtor(){
        System.out.println("Qual o nome do Ator?");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliações a partir de qual valor?");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = serieRepository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        System.out.println("Series em que " + nomeAtor + " trabalhou:");
        seriesEncontradas.forEach(s -> System.out.println("Título: " + s.getTitulo() + " Avaliação: " + s.getAvaliacao()));
    }

    private void buscarTop5Series(){
        List<Serie> seriesTop = serieRepository.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println("Título: " + s.getTitulo() + " Avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Deseja buscar series de quais categorias/gênero?");
        var nomeCategoria = leitura.nextLine();

        Categoria categoria = Categoria.fromProtugues(nomeCategoria);
        List<Serie> seriesPorCategoria = serieRepository.findByGenero(categoria);
        System.out.println("Series da categoria: " + categoria);
        seriesPorCategoria.forEach(System.out::println);
    }

    public void buscarSeriesPorNumTemporadasAvaliacao() {
        System.out.println("Qual o número máximo de temporadas? ");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Qual o número mínimo de Avaliação?");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesBuscadas = serieRepository.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        seriesBuscadas.forEach(System.out::println);
    }

    public void buscarEpisodiosPorTrecho(){
        System.out.println("Insira o Trecho para a busca");
        var trechoEpisodio = leitura.nextLine();

        List<Episodio> episodiosBuscados = serieRepository.episodiosPorTrecho(trechoEpisodio);

        episodiosBuscados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo()));
    }

    public void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = serieRepository.topEpisodiosPorSerie(serie);

            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s -> Avaliação: %f\n",
                            e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }

    private void buscarEpisodiosAposData() {
        buscarSeriePorTitulo();
        if(serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();

            System.out.println("Digite o ano limite de lançamento");
            var anoLimite = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = serieRepository.episodiosPorSerieEAno(serie, anoLimite);

            episodiosAno.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s -> Ano Lançamento: %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getDataLancamento().getYear()));
        }
    }
}