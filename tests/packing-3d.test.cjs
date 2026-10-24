const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const sandbox={};
vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../backend/src/main/resources/static/packing-geometry.js'),'utf8'),sandbox);
const G=sandbox.PackingGeometry;
test('a placement keeps API length, width and height on their original axes',()=>{
  const vertices=G.corners(30,40,50,120,80,60);
  assert.deepEqual(Array.from(vertices[0]),[30,40,50]);
  assert.deepEqual(Array.from(vertices[6]),[150,120,110]);
  assert.equal(vertices.length,8);
});
test('orbiting preserves the geometry and depth of the scene',()=>{
  const box={l:600,w:450,h:500};
  for(const yaw of [0,.7,2.1,4.3])for(const elevation of [.12,.48,Math.PI/2]){
    const a=G.project([10,20,30],box,yaw,elevation),b=G.project([140,70,110],box,yaw,elevation);
    const distance=(a.x-b.x)**2+(a.y-b.y)**2+(a.depth-b.depth)**2;
    assert.ok(Math.abs(distance-(130**2+50**2+80**2))<1e-7);
  }
});
test('the complete box is fitted inside a small viewport at the default zoom',()=>{
  const box={l:850,w:700,h:650};
  for(const yaw of [0,.6,2,4])for(const elevation of [.12,.48,Math.PI/2]){
    const fit=G.fit(box,330,300,yaw,elevation);
    for(const vertex of G.corners(0,0,0,box.l,box.w,box.h)){
      const p=G.project(vertex,box,yaw,elevation);
      assert.ok(fit.x+p.x*fit.scale>=35 && fit.x+p.x*fit.scale<=295);
      assert.ok(fit.y+p.y*fit.scale>=30 && fit.y+p.y*fit.scale<=270);
    }
  }
});
test('top view shows top surfaces and hides vertical sides',()=>{
  assert.equal(G.visible([0,0,1],0,Math.PI/2),true);
  assert.equal(G.visible([0,0,-1],0,Math.PI/2),false);
  assert.equal(G.visible([1,0,0],0,Math.PI/2),false);
  assert.equal(G.visible([0,1,0],0,Math.PI/2),false);
});
