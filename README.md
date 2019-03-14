# Knot.x Knot Engine


```htmló
<knotx:snippet flowAlias="default">
</knotx:snippet>
```

```html
<knotx:snippet flow="databridge,te">
</knotx:snippet>
```

```html
<knotx:snippet flow="FLOW">
</knotx:snippet>
```
where `FLOW` contains encoded string without new lines:
```json
{
  "stepAlias": "databridge",
  "onTransition": {
    "next": {
      "stepAlias": "te"
    }
  }
}
```

or

```json
{
  "step": {
    "address": "knotx.knot.databridge",
    "deliveryOptions": {
      "sendTimout": 1000
    }
  },
  "onTransition": {
    "next": {
      "step": {
        "address": "knotx.knot.te",
        "deliveryOptions": {
          "sendTimout": 1000
        }
      }
    }
  }
}
```