<template>
  <RouterView v-slot="{ Component }">
    <Transition name="root-fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
</template>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100vh;
  overflow: hidden;
}

.root-fade-enter-active,
.root-fade-leave-active {
  transition: opacity 0.28s cubic-bezier(0.22, 1, 0.36, 1), transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.root-fade-enter-from {
  opacity: 0;
  transform: scale(0.992);
}

.root-fade-leave-to {
  opacity: 0;
  transform: scale(1.005);
}
</style>
