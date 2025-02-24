#!/bin/bash

echo -n "Insert MacBook User Name : "
read UserName

echo "homebrew install"
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> /Users/${UserName}/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"

echo "install Oh My Zsh"
sh -c "$(curl -fsSL https://raw.githubusercontent.com/robbyrussell/oh-my-zsh/master/tools/install.sh)"

echo "install zsh-autosuggestion, zsh-syntax-highlighting, kubectx"
brew install zsh-autosuggestions
brew install zsh-syntax-highlighting
brew install kubectx

cat <<EOF >> .zshrc
source /opt/homebrew/share/zsh-autosuggestions/zsh-autosuggestions.zsh
source /opt/homebrew/share/zsh-syntax-highlighting/zsh-syntax-highlighting.zsh
EOF

echo "replace Thema >> robbyrrussell to agnoster"
sed -i '' 's/robbyrrussell/agnoster/g' ~/.zshrc

echo " setting emoji and kubectx"
cat <<EOF >> ~/.oh-my-zsh/themes/agnoster.zsh-theme
prompt_context() {
  if [[ "$USER" != "$DEFAULT_USER" || -n "$SSH_CLIENT" ]]; then
    prompt_segment black default "%(!.%{%F{yellow}%}.)$USER"
  fi
}

prompt_kubectx () {
  if [[ $(kubectx -c) == "orbstack" ]]; then
    prompt_segment bg yellow $(kubectx -c)%{%F{white}%}:%{%F{cyan}%}$(kubens -c)
  elif [[ $(kubectx -c) == "docker-desktop" ]]; then
    prompt_segment bg blue $(kubectx -c)%{%F{white}%}:%{%F{cyan}%}$(kubens -c)
  else
    prompt_segment bg red $(kubectx -c)%{%F{white}%}:%{%F{cyan}%}$(kubens -c)
  fi
}


get_random_emoji() {
  emojis=("⚡️" "🔥" "👑" "😎" "🐸" "🐵" "🦄" "🌈" "🍻" "🚀" "💡" "🎉" "🔑" "🚦" "🌙")
  local RAND_EMOJI_N=$(( $RANDOM % ${#emojis[@]} ))
  echo "${emojis[$RAND_EMOJI_N]}"
}


prompt_newline() {
  local random_emoji
  random_emoji=$(get_random_emoji)  # 랜덤 이모티콘 받기

  if [[ -n $CURRENT_BG ]]; then
    echo -n "%{%k%F{$CURRENT_BG}%} $random_emoji
%{%k%F{blue}%}$SEGMENT_SEPARATOR"
  else
    echo -n "%{%k%}"
  fi

  echo -n "%{%f%}"
  CURRENT_BG=''
}
EOF

sed -i '' '/build_prompt()/,/prompt_end/ {
  /build_prompt()/, /prompt_hg/ {
    /prompt_hg/ a\
    prompt_kubectx\
    prompt_newline
  }
}' "~/.oh-my-zsh/themes/agnoster.zsh-theme"

echo "refresh Oh My Zsh"
source .zshrc