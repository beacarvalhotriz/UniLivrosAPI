package com.unilivros.service;

import com.unilivros.dto.LivroDTO;
import com.unilivros.exception.ResourceNotFoundException;
import com.unilivros.model.Livro;
import com.unilivros.model.Usuario;
import com.unilivros.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecomendacaoServiceImpl implements RecomendacaoService {

    private final UsuarioRepository usuarioRepository;
    private final LivroRepository livroRepository;
    private final UsuarioLivroRepository usuarioLivroRepository;
    private final PropostaRepository propostaRepository;
    private final TrocaRepository trocaRepository;
    private final ModelMapper modelMapper;

    public RecomendacaoServiceImpl(UsuarioRepository usuarioRepository,
                                   LivroRepository livroRepository,
                                   UsuarioLivroRepository usuarioLivroRepository,
                                   PropostaRepository propostaRepository,
                                   TrocaRepository trocaRepository,
                                   ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.livroRepository = livroRepository;
        this.usuarioLivroRepository = usuarioLivroRepository;
        this.propostaRepository = propostaRepository;
        this.trocaRepository = trocaRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LivroDTO> recomendarParaUsuario(Long usuarioId, int limite) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado, id, " + usuarioId));

        // 1, buscar livros com os quais o usuario ja se relacionou, possui, propôs, trocou
        Set<Long> livrosDoUsuario = buscarLivrosDoUsuario(usuario);
        Set<Long> livrosEmPropostas = buscarLivrosDePropostas(usuario);
        Set<Long> livrosEmTrocas = buscarLivrosDeTrocas(usuario);

        // conjunto de livros que indicam interesse historico
        Set<Long> livrosInteressantes = new HashSet<>();
        livrosInteressantes.addAll(livrosDoUsuario);
        livrosInteressantes.addAll(livrosEmPropostas);
        livrosInteressantes.addAll(livrosEmTrocas);

        if (livrosInteressantes.isEmpty()) {
            // se nao tem historico, devolve livros mais populares ou recentes
            return recomendarPorPopularidade(limite);
        }

        // 2, coletar generos e possiveis features dos livros que o usuario ja gosta
        List<Livro> basePreferencias = livroRepository.findAllById(livrosInteressantes);

        Set<String> generosPreferidos = basePreferencias.stream()
                .map(Livro::getGenero)         // ajuste se o campo tiver outro nome
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 3, buscar outros livros do mesmo genero que o usuario ainda nao tem nem trocou
        List<Livro> candidatos = livroRepository.findAll().stream()
                .filter(livro -> livro.getGenero() != null)
                .filter(livro -> !livrosInteressantes.contains(livro.getId()))
                .filter(livro -> generosPreferidos.contains(livro.getGenero().toLowerCase()))
                .limit(limite)
                .collect(Collectors.toList());

        // 4, se tiver poucos candidatos, completa com popularidade simples
        if (candidatos.size() < limite) {
            List<LivroDTO> recomendados = candidatos.stream()
                    .map(l -> modelMapper.map(l, LivroDTO.class))
                    .collect(Collectors.toList());

            List<LivroDTO> populares = recomendarPorPopularidade(limite)
                    .stream()
                    .filter(dto -> recomendados.stream().noneMatch(r -> r.getId().equals(dto.getId())))
                    .collect(Collectors.toList());

            recomendados.addAll(populares);
            return recomendados.stream().limit(limite).collect(Collectors.toList());
        }

        return candidatos.stream()
                .map(l -> modelMapper.map(l, LivroDTO.class))
                .collect(Collectors.toList());
    }

    // metodos auxiliares, voce vai ajustar de acordo com seus campos e repositories

    private Set<Long> buscarLivrosDoUsuario(Usuario usuario) {
        // aqui voce usa UsuarioLivroRepository, por exemplo,
        // algo como findByUsuarioId(usuario.getId())
        // e extrai os ids dos livros
        return new HashSet<>();
    }

    private Set<Long> buscarLivrosDePropostas(Usuario usuario) {
        // aqui voce usa PropostaRepository, LivroPropostaRepository se tiver,
        // para pegar livros ofertados e solicitados pelo usuario
        return new HashSet<>();
    }

    private Set<Long> buscarLivrosDeTrocas(Usuario usuario) {
        // aqui voce usa TrocaRepository e TrocaUsuarioRepository para pegar
        // livros de trocas concluidas pelo usuario
        return new HashSet<>();
    }

    private List<LivroDTO> recomendarPorPopularidade(int limite) {
        // estrategia bem simples,
        // por enquanto, por exemplo, os livros mais recentes ou aleatorios
        List<Livro> livros = livroRepository.findAll()
                .stream()
                .limit(limite)
                .collect(Collectors.toList());

        return livros.stream()
                .map(l -> modelMapper.map(l, LivroDTO.class))
                .collect(Collectors.toList());
    }
}
