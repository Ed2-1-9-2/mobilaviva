// Date structurate (schema.org, JSON-LD) adăugate în <head> în funcție de pagină.
import { imgSrc } from './store'
const origin = () => location.origin
const abs = u => (u.startsWith('/') ? origin() + u : u)

export function setLd(id, data) {
  let el = document.getElementById('ld-' + id)
  if (!data) { if (el) el.remove(); return }
  if (!el) { el = document.createElement('script'); el.type = 'application/ld+json'; el.id = 'ld-' + id; document.head.appendChild(el) }
  el.textContent = JSON.stringify(data)
}

export const orgLd = () => ({ '@context': 'https://schema.org', '@type': 'Organization', name: 'MobilaViva', url: origin(), email: 'contact@mobilaviva.com' })

export const siteLd = () => ({
  '@context': 'https://schema.org', '@type': 'WebSite', name: 'MobilaViva', url: origin(),
  potentialAction: { '@type': 'SearchAction', target: origin() + '/catalog?q={search_term_string}', 'query-input': 'required name=search_term_string' }
})

// items: [{ name, path }]
export const crumbsLd = items => ({
  '@context': 'https://schema.org', '@type': 'BreadcrumbList',
  itemListElement: items.map((c, i) => ({ '@type': 'ListItem', position: i + 1, name: c.name, item: origin() + c.path }))
})

// aggregateRating și review se emit doar din recenziile reale ale clienților (rs = răspunsul /api/products/{id}/reviews); fără recenzii nu se emit.
export const productLd = (p, path, rs) => ({
  '@context': 'https://schema.org', '@type': 'Product', name: p.name, description: p.description, sku: String(p.id),
  brand: { '@type': 'Brand', name: p.brand }, category: p.category, material: p.material, color: p.color, image: abs(imgSrc(p)),
  ...(p.manufacturer ? { manufacturer: { '@type': 'Organization', name: p.manufacturer } } : {}),
  ...(p.originCountry ? { countryOfOrigin: { '@type': 'Country', name: p.originCountry } } : {}),
  ...(rs && rs.count ? {
    aggregateRating: { '@type': 'AggregateRating', ratingValue: String(rs.avg), reviewCount: rs.count, bestRating: '5', worstRating: '1' },
    review: rs.items.slice(0, 5).map(r => ({
      '@type': 'Review', author: { '@type': 'Person', name: r.author }, reviewRating: { '@type': 'Rating', ratingValue: r.stars, bestRating: 5, worstRating: 1 },
      ...(r.text ? { reviewBody: r.text } : {})
    }))
  } : {}),
  offers: {
    '@type': 'Offer', url: origin() + path, priceCurrency: 'RON', price: Number(p.price).toFixed(2),
    availability: p.stock > 0 ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock', itemCondition: 'https://schema.org/NewCondition'
  }
})
