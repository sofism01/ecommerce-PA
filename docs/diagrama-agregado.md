# Documentación del Agregado — BOHEME

## 1. Raíz de Agregado Identificada
* **Entidad Raíz:** `BolsaDeEstilo`
* **Justificación:** Es la entidad principal que controla la consistencia transaccional de los productos seleccionados por el cliente antes de proceder con el pago, gestionando su propio ciclo de vida y protegiendo los elementos internos de modificaciones externas no autorizadas.

## 2. Delimitación del Agregado
* **Dentro del límite (Objetos que cambian junto con la raíz):**
  * `BolsaDeEstilo` (Raíz)
  * `BolsaDeEstiloId` (Value Object de identidad)
  * `LineaDeBolsa` (Value Object / componente interno que agrupa cantidad y precio por variante)
* **Fuera del límite (Entidades externas referenciadas solo por ID):**
  * `VarianteDeTalla` (Referenciada mediante `varianteId`)
  * `Creador` (Referenciado mediante `creadorId` para el cálculo desagregado de envíos por atelier)

## 3. Invariantes del Agregado (Reglas de Negocio Garantizadas)
1. **Consistencia de stock:** Una bolsa de estilo **nunca puede** registrar cantidades de una variante de talla que excedan el inventario físico disponible en el taller del creador al momento de agregarlas.
2. **Inmutabilidad de la identidad:** La identidad de la bolsa (`BolsaDeEstiloId`) y el identificador del cliente (`clienteId`) **siempre deben** permanecer constantes durante todo su ciclo de vida (la clase no incluye métodos *setters*).
3. **Coherencia monetaria:** Todas las líneas dentro de la bolsa **siempre deben** calcular sus subtotales utilizando estrictamente el objeto de valor `Dinero`, evitando mezclas de monedas o errores de precisión numérica.
4. **Bloqueo post-transacción:** Una bolsa de estilo que ha completado su transición y se ha convertido formalmente en un `PedidoDeModa` **nunca puede** aceptar modificaciones, adiciones ni eliminaciones en su contenido interno.
