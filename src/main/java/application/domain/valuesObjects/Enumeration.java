package application.domain.valuesObjects;

/**
 * Contract of the business catalogues of the domain (RD-VO-10, RD-VO-11).
 *
 * <p>Catalogues are closed sets of values: any value outside the catalogue is an invalid state of
 * the domain. They are modelled as Java enums implementing this contract because the language
 * forbids {@code enum X extends Y}, and a Java enum already guarantees the whole
 * {@link ValueObject} contract by construction: immutability, absence of identity, equality by
 * value and a closed set of instances.</p>
 */
public interface Enumeration {

    /**
     * @return stable code of the catalogue value.
     */
    String code();

    /**
     * @return business meaning of the catalogue value.
     */
    String description();
}
