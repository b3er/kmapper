KMapper [![Jitpack](https://jitpack.io/v/b3er/kmapper.svg)](https://jitpack.io/v/b3er/kmapper) [![](https://jitci.com/gh/b3er/kmapper/svg)](https://jitci.com/gh/b3er/kmapper)
===
Dumb and simple implementation of data class mapper on KSP. WIP - not for production, just POC.

### Goals

* Replacement of mapstruct for kotlin

### Implemented

- [x] data class mapping
- [x] nested mappings reuse and generation
- [x] nullability
- [x] iterable mappings
- [x] enum mappings
- [x] default values
- [ ] generic data classes

Download
---

```kotlin
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

plugins {
    ...
    id("com.google.devtools.ksp") version "<version>"
}

dependencies {
    val kMapperVersion = "0.3.10"
    implementation("com.github.b3er.kmapper:api:$kMapperVersion")
    ksp("com.github.b3er.kmapper:processor:$kMapperVersion")
}
```

License
---

```text
Copyright 2021 Ilya Usanov.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
