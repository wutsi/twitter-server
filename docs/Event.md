# Events Emitted
Here are the events emitted by this service

### Event: Shared
| URN | `urn:event:wutsi:twitter:shared` |
|-----|----------------------------------|
| Description | This event is emitted when a story is shared on Twitter |

##### Event Payload
```json
{
  "twitterStatusId": 123320932, // ID of the tweet
  "postId": 123                 // OPTIONAL: Id of the associated post
}
```

# Events Consumed
Here are the events consumed by this service

| Event URN | Source | Action |
|-----------|--------|------------------------|
| `urn:event:wutsi:story:published` | story | The published story will be shared on Twitter |
| `urn:event:wutsi:channel:secret-submitted` | channel | The user access-token and secret will be stored into the database |
| `urn:event:wutsi:channel:secret-revoked` | channel | The user access-token and secret will be remove from the database |
| `urn:event:wutsi:post:submitted` | post | Post submitted for sharing on social media |



