import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let previousCommand

const generateTimeStamp = () => {
  const d = new Date()
  const dayWord = (d.toString().split(' '))[0]
  const month = d.getMonth() + 1
  const day = d.getDate()
  const year = d.getFullYear()
  let hour = d.getHours()
  let night
  if (hour === 12)
  {
    night = 'PM'
  }
  else if (hour < 12)
  {
    night = 'AM'
  }
  else if (hour > 12)
  {
    hour = hour - 12
    night = 'PM'
  }
  let minutes = String(d.getMinutes());
  let seconds = String(d.getSeconds());
  if (minutes.length < 2)
  {
    minutes = 0 + minutes;
  }
  if (seconds.length < 2)
  {
    seconds = 0 + seconds
  }
  

  const timestamp = dayWord + ' (' + month + '\/' + day + '\/' + year + ')' + ' ' + hour + ':' + minutes + ':' + seconds + night
  return timestamp
}


cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .description('Connects to a server with given host and port')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    const argsHost = args.host !== undefined ? args.host : 'localhost'
    const argsPort = args.port !== undefined ? args.port : 8080
    const timestamp = generateTimeStamp()
    server = connect({ host: argsHost, port: argsPort }, () => {
      server.write(new Message({ username, command: 'connect', undefined, timestamp}).toJSON() + '\n')
      callback()
    })

    

    server.on('error', (err) => {
      if (err.code === 'ECONNREFUSED')
      {
        this.log('Server(\'' + argsHost + '\', \'' + argsPort + '\') is not currently active')
      }
      else if (err.code === 'ECONNRESET')
      {
        this.log('Server(\'' + argsHost + '\', \'' + argsPort + '\') has been shut down')
      }
      cli.exec('exit')
      callback()
    })

    server.on('data', (buffer) => {
      const message = Message.fromJSON(buffer)
      if (message.command === 'connect')
      {
        if (message.error === true)
        {
          this.log(cli.chalk['green']('`' + message.timestamp + ': ' + message.contents + '`'))
        }
        else
        {
          this.log(cli.chalk['green']('`' + message.timestamp + ': <' + message.username + '> has connected`'))
        }
      }
      else if (message.command === 'disconnect')
      {
        this.log(cli.chalk['green']('`' + message.timestamp + ': <' + message.username + '> has disconnected`'))
      }
      else if (message.command === 'echo')
      {
        this.log(cli.chalk['magenta']('`' + message.timestamp + ' <' + message.username + '> ' + '(echo): ' + message.contents + '`'))
      }
      else if (message.command === 'broadcast')
      {
        this.log(cli.chalk['yellow']('`' + message.timestamp + ' <' + message.username + '> ' + '(all): ' + message.contents + '`'))
      }
      else if (String(message.command).startsWith('@') === true)
      {
        if (message.error === true)
        {
          this.log(cli.chalk['blue']('`' + message.timestamp + ': ' + message.contents + '`'))
        }
        else
        {
          this.log(cli.chalk['blue']('`' + message.timestamp + ' <' + message.username + '> ' + '(whisper): ' + message.contents + '`'))
        }
      }
      else if (message.command === 'users')
      {
        this.log('`' + message.timestamp + ': ' + 'currently connected users:`' + message.contents)
      }
      else if (message.command === 'help')
      {
        this.log(message.contents);
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input = undefined, callback) {
    const [ command, ...rest ] = input.split(' ')
    let contents = rest.join(' ')
    const timestamp = generateTimeStamp()

    const evaluateCommand = (command) => {
      if (command === 'disconnect') {
        previousCommand = null
        server.end(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'echo' || command === 'broadcast' || String(command).startsWith('@') === true) {
        previousCommand = command
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else if (command === 'users' || command === 'help') {
        server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
      } else {
        if (previousCommand)
        {
          contents = (command + ' ' + contents).trim()
          evaluateCommand(previousCommand)
        }
        else
        {
          this.log(`\n  Invalid Command. Showing Help:`)
          evaluateCommand('help')
        }
      }

    }

    evaluateCommand(command);

    callback()
  })

  
