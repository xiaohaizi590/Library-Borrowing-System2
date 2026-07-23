<template>
  <span
    :class="badgeClass"
    class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
  >
    {{ text }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, required: true },
  map: { type: Object, required: true }
})

const badgeClass = computed(() => {
  return props.map[props.type]?.class || 'bg-gray-100 text-gray-800'
})

const text = computed(() => {
  return props.map[props.type]?.text || props.type
})
</script>
