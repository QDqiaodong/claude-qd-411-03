import { createRouter, createWebHistory } from 'vue-router'
import Plot from '../views/Plot.vue'
import Tree from '../views/Tree.vue'
import Spray from '../views/Spray.vue'
import HarvestBatch from '../views/HarvestBatch.vue'
import Inventory from '../views/Inventory.vue'

const routes = [
  { path: '/', redirect: '/plots' },
  { path: '/plots', name: '地块', component: Plot },
  { path: '/trees', name: '果树', component: Tree },
  { path: '/sprays', name: '施药记录', component: Spray },
  { path: '/harvest-batches', name: '采摘批次', component: HarvestBatch },
  { path: '/inventory', name: '库存', component: Inventory }
]

export default createRouter({ history: createWebHistory(), routes })
