package com.jlrm.springboot.app.oauth.service;

import com.jlrm.springboot.app.usuarios.commons.models.entity.Usuario;

public interface IUsuarioService {
	
	public Usuario findByUsername(String username);
	
	public Usuario update(Usuario usuario, Long id);

}
