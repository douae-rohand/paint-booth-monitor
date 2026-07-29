package com.projet.measures.exception;

/**
 * Exception levée lorsqu'une métrique n'est pas applicable à un point de mesure.
 * Par exemple : HUMIDITE pour un point de type ETUVE (seulement TEMPERATURE applicable).
 */
public class MetriqueNonApplicableAuPointException extends RuntimeException {
    public MetriqueNonApplicableAuPointException(String message) {
        super(message);
    }
}
