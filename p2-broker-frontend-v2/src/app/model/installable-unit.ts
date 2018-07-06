
export class InstallableUnit {
    public readonly id: string;

    constructor(public readonly name: string, public readonly version: string) {
        this.id = name + ":" + version;
    }
}