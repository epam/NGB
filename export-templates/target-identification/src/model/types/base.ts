export type LinkValue = {
  value: string;
  link?: string;
}

export type ItemSimpleValue = string | number | boolean | undefined;

export type ItemValue = ItemSimpleValue | LinkValue;

export type KeyOfType<Item, Type> = (keyof {
  [P in keyof Item as Item[P] extends Type ? (P & string) : never]: any;
}) & string;

export type ItemProperty<Item> = KeyOfType<Item, ItemValue>;

export type ItemValueFn<Item> = (item: Item) => ItemValue;
