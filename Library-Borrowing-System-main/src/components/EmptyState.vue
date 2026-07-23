<template>
  <div class="text-center py-12">
    <component :is="iconComponent" class="w-16 h-16 text-gray-300 mx-auto mb-4" />
    <p class="text-gray-500">{{ message }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BookX, BookOpenCheck, ClipboardList } from 'lucide-vue-next'

const props = defineProps({
  message: { type: String, required: true },
  icon: {
    type: [String, Object, Function],
    default: 'BookX'
  }
})

const iconMap = { BookX, BookOpenCheck, ClipboardList }

const iconComponent = computed(() => {
  if (typeof props.icon === 'string') {
    return iconMap[props.icon] || BookX
  }
  return props.icon
})
</script>
