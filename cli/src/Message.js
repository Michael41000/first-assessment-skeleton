export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, timestamp, error}) {
    this.username = username
    this.command = command
    this.contents = contents
    this.timestamp = timestamp
    this.error = error
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      timestamp: this.timestamp,
      error : this.error
    })
  }

  toString () {
    return this.contents
  }
}
