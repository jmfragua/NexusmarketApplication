import type { ProductResponse } from '@/api/types'

export const SAMPLE_PREFIX = 'muestra-'

export function isSampleId(productId: string): boolean {
  return productId.startsWith(SAMPLE_PREFIX)
}

function sample(
  slug: string,
  productType: ProductResponse['productType'],
  variants: Array<Record<string, string>>,
): ProductResponse {
  return {
    id: `${SAMPLE_PREFIX}${slug}`,
    sellerId: 'muestra',
    productType,
    status: 'PUBLISHED',
    visibleInCatalog: true,
    variants: variants.map((attributes, i) => ({ id: `${SAMPLE_PREFIX}${slug}-v${i + 1}`, attributes })),
  }
}

/** Shown only when the backend is unreachable or its catalogue is empty, always with a notice. */
export const SAMPLE_PRODUCTS: ProductResponse[] = [
  sample('morral', 'PHYSICAL', [
    { name: 'Morral urbano 22 L', brand: 'Páramo', price: '189000', currency: 'COP', description: 'Lona encerada, bolsillo acolchado para portátil de 15" y costuras selladas.', Color: 'Arena', image: '/productos/morral.jpg' },
    { name: 'Morral urbano 22 L', brand: 'Páramo', price: '189000', currency: 'COP', Color: 'Carbón' },
  ]),
  sample('audifonos', 'PHYSICAL', [
    { name: 'Audífonos inalámbricos Onda', brand: 'Kilo', price: '249900', currency: 'COP', description: 'Cancelación de ruido, 40 horas de batería y carga rápida por USB-C.', Color: 'Cobalto', image: '/productos/audifonos.jpg' },
    { name: 'Audífonos inalámbricos Onda', brand: 'Kilo', price: '259900', currency: 'COP', Color: 'Lima' },
  ]),
  sample('cafe', 'PHYSICAL', [
    { name: 'Café de origen Nariño', brand: 'Tostadero Sur', price: '42000', currency: 'COP', description: 'Notas de panela y cítricos. Tueste medio, molido o en grano.', Presentación: '340 g en grano', image: '/productos/cafe.jpg' },
    { name: 'Café de origen Nariño', brand: 'Tostadero Sur', price: '42000', currency: 'COP', Presentación: '340 g molido' },
  ]),
  sample('curso', 'DIGITAL', [
    { name: 'Curso de fotografía con celular', brand: 'Estudio Faro', price: '89000', currency: 'COP', description: '12 lecciones en video, acceso inmediato al confirmar el pago.', image: '/productos/curso.jpg' },
  ]),
  sample('tenis', 'PHYSICAL', [
    { name: 'Tenis de lona Bahía', brand: 'Malecón', price: '159000', currency: 'COP', description: 'Suela vulcanizada y plantilla de espuma reciclada.', Talla: '38', image: '/productos/tenis.jpg' },
    { name: 'Tenis de lona Bahía', brand: 'Malecón', price: '159000', currency: 'COP', Talla: '40' },
    { name: 'Tenis de lona Bahía', brand: 'Malecón', price: '159000', currency: 'COP', Talla: '42' },
  ]),
  sample('ebook', 'DIGITAL', [
    { name: 'Libro digital: cocina de mercado', brand: 'Editorial Plaza', price: '35000', currency: 'COP', description: '80 recetas de temporada en PDF y EPUB.', image: '/productos/libro.jpg' },
  ]),
  sample('lampara', 'PHYSICAL', [
    { name: 'Lámpara de escritorio Eje', brand: 'Taller Norte', price: '134500', currency: 'COP', description: 'Brazo articulado de aluminio y luz cálida regulable.', Acabado: 'Coral', image: '/productos/lampara.jpg' },
  ]),
  sample('plantilla', 'DIGITAL', [
    { name: 'Plantillas de presupuesto personal', brand: 'Cuentas Claras', price: '19900', currency: 'COP', description: 'Hojas de cálculo listas para usar, con guía de inicio.', image: '/productos/plantillas.jpg' },
  ]),
]
