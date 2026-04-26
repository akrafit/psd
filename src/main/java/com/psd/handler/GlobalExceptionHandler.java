package com.psd.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex, HttpServletRequest request) {
        logger.error("Global exception handler caught exception for URL: {}", request.getRequestURL(), ex);

        ModelAndView mav = new ModelAndView();
        mav.addObject("error", ex.getMessage());
        mav.addObject("url", request.getRequestURL());
        mav.addObject("method", request.getMethod());
        mav.setViewName("error");

        return mav;
    }
}