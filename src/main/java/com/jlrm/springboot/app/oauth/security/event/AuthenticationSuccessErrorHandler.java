package com.jlrm.springboot.app.oauth.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.jlrm.springboot.app.oauth.service.IUsuarioService;
import com.jlrm.springboot.app.usuarios.commons.models.entity.Usuario;

import brave.Tracer;
import feign.FeignException;

@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher{

	private Logger log = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);
	
	@Autowired
	private IUsuarioService usuarioService;
	
	@Autowired
	private Tracer tracer;
	
	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {
		UserDetails user = (UserDetails) authentication.getPrincipal();
		String mensaje = "Success login "+user.getUsername();
		System.out.println(mensaje);
		log.info(mensaje);
		
		Usuario usuario = usuarioService.findByUsername(authentication.getName());
		
		if (null != usuario.getIntentos() && usuario.getIntentos() > 0) {
			usuario.setIntentos(0);
			usuarioService.update(usuario, usuario.getId());
		}
		
	}

	@Override
	public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
		String mensaje = "Error en login ** " + exception.getMessage();
		log.error(mensaje);
		System.out.println(mensaje);
		
		try {
			StringBuilder errors = new StringBuilder();
			errors.append(mensaje);
			Usuario usuario = usuarioService.findByUsername(authentication.getName());
			
			if (null == usuario.getIntentos()) {
				usuario.setIntentos(0);
			}
			
			log.info("Intentos actuales "+ usuario.getIntentos());
			
			usuario.setIntentos(usuario.getIntentos()+1);
			
			log.info("Intentos actualizados "+ usuario.getIntentos());
			
			errors.append(" - Intentos del login "+ usuario.getIntentos());
			
			if (usuario.getIntentos() >= 3 ) {
				String maxError = String.format("El usuario %s deshabilitado por máximo numero de intentos.", usuario.getUsername());
				log.error(maxError);
				errors.append(" - "+maxError);
				usuario.setEnabled(false);
			}
			
			usuarioService.update(usuario, usuario.getId());
			
			tracer.currentSpan().tag("error.mensaje", errors.toString());
			
		}catch(FeignException fe) {
			log.error(String.format("Usuario %s no existe en sistema", authentication.getName()));
		}
		
		
	}

}
