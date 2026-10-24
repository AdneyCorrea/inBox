(() => {
  const normalize=value=>String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[-_+/]/g,' ').replace(/\s+/g,' ').trim();
  const compile=pattern=>new RegExp('(?:^|\\b)(?:'+pattern+')(?:\\b|$)','i');
  const rules=(globalThis.CatalogKeywordRules||[]).map(rule=>({...rule,patterns:rule.patterns.map(compile),exclude:rule.exclude.map(compile)}));
  globalThis.suggestCatalogTopic=name=>{
    const text=normalize(name).replace(/^(?:openbox|open box|reembalado|produto novo)\s+/,'');
    if(text.length<3)return '';
    const candidates=[];
    for(const rule of rules){
      if(rule.exclude.some(pattern=>pattern.test(text)))continue;
      const hits=rule.patterns.map(pattern=>pattern.exec(text)).filter(Boolean);
      if(!hits.length)continue;
      const first=Math.min(...hits.map(hit=>hit.index));
      const score=rule.priority+Math.max(0,120-first*3);
      candidates.push({id:rule.id,score});
    }
    candidates.sort((a,b)=>b.score-a.score);
    if(candidates.length){
      if(candidates[1]&&candidates[0].score-candidates[1].score<15)return '';
      return candidates[0].id;
    }
    const exact=(globalThis.CatalogDepartments||[]).flatMap(d=>d.topics).filter(t=>normalize(t.label)===text);
    return exact.length===1?exact[0].id:'';
  };
})();
