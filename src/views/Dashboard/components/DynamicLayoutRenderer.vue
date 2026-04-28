<template>
  <div class="dynamic-layout-renderer">
    <div v-for="col in layout.columns" :key="col.id || Math.random()" class="dynamic-column" :style="{ width: (col.width || 12) / 24 * 100 + '%' }">
      <component
        v-for="widget in col.widgets"
        :key="widget.id || Math.random()"
        :is="resolveComponent(widget.component)"
        v-bind="getProps(widget.component)"
        v-on="getListeners(widget.component)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  layout: { type: Object, default: () => ({ columns: [] }) },
  registry: { type: Object, required: true },
  widgetProps: { type: Object, default: () => ({}) },
  widgetListeners: { type: Object, default: () => ({}) }
})

const resolveComponent = (name) => props.registry[name]

const getProps = (name) => {
  return props.widgetProps[name] || {}
}

const getListeners = (name) => {
  return props.widgetListeners[name] || {}
}
</script>

<style scoped>
.dynamic-layout-renderer {
  display: flex;
  gap: 20px;
  width: 100%;
}
.dynamic-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
