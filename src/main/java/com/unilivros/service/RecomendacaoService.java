package com.unilivros.service;

import com.unilivros.dto.LivroDTO;
import java.util.List;

public interface RecomendacaoService {

    List<LivroDTO> recomendarParaUsuario(Long usuarioId, int limite);
}
