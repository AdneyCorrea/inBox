// Fornece cálculos geométricos e projeções usados na visualização 3D das embalagens.
/* O desenho usa os mesmos eixos da API: X = comprimento, Y = largura, Z = altura.
 * Girar a câmera muda só a visão; as posições calculadas pelo servidor ficam iguais. */
(() => {
  const corners = (x, y, z, l, w, h) => [
    [x, y, z],
    [x + l, y, z],
    [x + l, y + w, z],
    [x, y + w, z],
    [x, y, z + h],
    [x + l, y, z + h],
    [x + l, y + w, z + h],
    [x, y + w, z + h],
  ];
  const faces = [
    { indices: [0, 3, 2, 1], normal: [0, 0, -1], shade: 0.72 },
    { indices: [4, 5, 6, 7], normal: [0, 0, 1], shade: 1.08 },
    { indices: [0, 1, 5, 4], normal: [0, -1, 0], shade: 0.88 },
    { indices: [1, 2, 6, 5], normal: [1, 0, 0], shade: 0.92 },
    { indices: [2, 3, 7, 6], normal: [0, 1, 0], shade: 0.78 },
    { indices: [3, 0, 4, 7], normal: [-1, 0, 0], shade: 0.84 },
  ];
  const edges = [
    [0, 1],
    [1, 2],
    [2, 3],
    [3, 0],
    [4, 5],
    [5, 6],
    [6, 7],
    [7, 4],
    [0, 4],
    [1, 5],
    [2, 6],
    [3, 7],
  ];
  function project(point, box, yaw, elevation) {
    const x = point[0] - box.l / 2,
      y = point[1] - box.w / 2,
      z = point[2] - box.h / 2;
    const cy = Math.cos(yaw),
      sy = Math.sin(yaw),
      ce = Math.cos(elevation),
      se = Math.sin(elevation);
    return {
      x: cy * x - sy * y,
      y: se * (sy * x + cy * y) - ce * z,
      depth: ce * (sy * x + cy * y) + se * z,
    };
  }
  function fit(box, width, height, yaw, elevation, zoom = 1) {
    const projected = corners(0, 0, 0, box.l, box.w, box.h).map((p) =>
      project(p, box, yaw, elevation),
    );
    const xs = projected.map((p) => p.x),
      ys = projected.map((p) => p.y);
    const minX = Math.min(...xs),
      maxX = Math.max(...xs),
      minY = Math.min(...ys),
      maxY = Math.max(...ys);
    const scale =
      Math.min(
        Math.max(1, width - 80) / (maxX - minX || 1),
        Math.max(1, height - 75) / (maxY - minY || 1),
      ) * zoom;
    return {
      scale,
      x: width / 2 - ((minX + maxX) * scale) / 2,
      y: height / 2 - ((minY + maxY) * scale) / 2 - 3,
    };
  }
  function visible(normal, yaw, elevation) {
    return (
      normal[0] * Math.sin(yaw) * Math.cos(elevation) +
        normal[1] * Math.cos(yaw) * Math.cos(elevation) +
        normal[2] * Math.sin(elevation) >
      1e-7
    );
  }
  globalThis.PackingGeometry = { corners, faces, edges, project, fit, visible };
})();
