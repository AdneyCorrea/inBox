// Normaliza termos e classifica produtos por relevância para a busca no catálogo.
(() => {
  const normalize = (value) =>
    String(value || "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .replace(/([a-z])([0-9])/g, "$1 $2")
      .replace(/([0-9])([a-z])/g, "$1 $2")
      .replace(/[^a-z0-9]+/g, " ")
      .trim();
  const compact = (value) => normalize(value).replace(/ /g, "");
  const stem = (word) => word.replace(/(?:es|s)$/, "");
  function oneEdit(a, b) {
    if (a === b) return true;
    if (Math.abs(a.length - b.length) > 1) return false;
    let i = 0,
      j = 0,
      errors = 0;
    while (i < a.length && j < b.length) {
      if (a[i] === b[j]) {
        i++;
        j++;
        continue;
      }
      if (++errors > 1) return false;
      if (a.length >= b.length) i++;
      if (b.length >= a.length) j++;
    }
    return errors + (i < a.length || j < b.length ? 1 : 0) <= 1;
  }
  globalThis.searchPackingProducts = (products, query, options) => {
    const pool = products.filter(
      (p) =>
        (!options.department || options.topicId(p).startsWith(options.department + "/")) &&
        (!options.topic || options.topicId(p) === options.topic),
    );
    const q = normalize(query),
      code = compact(query);
    if (!q) return { products: pool, similar: false };
    const words = q.split(" ");
    const ranked = pool
      .map((p, index) => {
        const name = normalize(p.name),
          sku = normalize(p.sku),
          skuCode = compact(p.sku);
        const text = name + " " + sku + " " + normalize(options.topicLabel(p));
        let score = 0;
        if (skuCode && skuCode === code) score = 1000;
        else if (skuCode && skuCode.startsWith(code)) score = 800;
        else if (name === q) score = 700;
        else if (words.every((w) => (name + " " + sku).includes(w)))
          score = 500 + (name.startsWith(q) ? 50 : 0);
        else if (words.every((w) => text.includes(w))) score = 300;
        return { p, index, score };
      })
      .filter((x) => x.score)
      .sort((a, b) => b.score - a.score || a.index - b.index);
    if (ranked.length) return { products: ranked.map((x) => x.p), similar: false };
    const inferred = globalThis.suggestCatalogTopic?.(query);
    if (inferred && !/[0-9]/.test(q)) {
      const inferredProducts = pool.filter((p) => options.topicId(p) === inferred);
      if (inferredProducts.length) return { products: inferredProducts, similar: false };
    }
    // Typo tolerance only for words, never for SKUs or numeric models.
    const similar = pool.filter((p) => {
      const tokens = normalize(p.name + " " + options.topicLabel(p)).split(" ");
      return words.every(
        (w) =>
          tokens.some((t) => t.includes(w)) ||
          (/^[a-z]{5,}$/.test(w) &&
            tokens.some((t) => /^[a-z]{5,}$/.test(t) && oneEdit(stem(w), stem(t)))),
      );
    });
    return { products: similar, similar: similar.length > 0 };
  };
})();
